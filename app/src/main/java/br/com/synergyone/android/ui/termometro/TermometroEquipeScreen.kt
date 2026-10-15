package br.com.synergyone.android.ui.termometro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AvaliacaoEquipeDto
import br.com.synergyone.android.data.termometro.AvaliacaoForm
import kotlinx.coroutines.launch
import br.com.synergyone.android.data.api.EmpresaTermometroDto
import br.com.synergyone.android.data.api.TermometroEquipeDto
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.termometro.TermometroRepository
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.common.LoadState
import br.com.synergyone.android.ui.common.LoadStateContent
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.common.formatPeriod
import br.com.synergyone.android.ui.common.formatPeriodShort
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.ui.components.BadgeTone
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.SectionTitle
import br.com.synergyone.android.ui.components.StatusBadge
import br.com.synergyone.android.ui.components.SynergyCard

/**
 * Painel do Termômetro para a equipe: empresa e período escolhidos pelo usuário, resultados ao
 * vivo, metas dos métodos e avaliações (rascunho/publicada). Gravar e publicar virão em seguida;
 * `podeEditar`/`podePublicar` já chegam do servidor para esconder ações.
 */
@Composable
fun TermometroEquipeScreen(
    repository: TermometroRepository,
    user: AuthenticatedUser,
    onAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    fixedEmpresaId: Long? = null,
    onBack: (() -> Unit)? = null,
) {
    var empresaId by rememberSaveable { mutableStateOf<Long?>(fixedEmpresaId) }
    var desde by rememberSaveable { mutableStateOf<String?>(null) }
    var ate by rememberSaveable { mutableStateOf<String?>(null) }
    var periodLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var periodoSheet by rememberSaveable { mutableStateOf(false) }

    val loader = rememberLoader("termometro-equipe-$empresaId-$desde-$ate") { repository.painelEquipe(empresaId, desde, ate) }
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<Pair<Long?, AvaliacaoForm>?>(null) }
    var confirmarPublicar by remember { mutableStateOf<Long?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    val state by loader.state.collectAsState()
    val dados = (state as? LoadState.Loaded)?.data

    // Mantém as empresas à vista enquanto o período/empresa novo carrega.
    var empresas by remember { mutableStateOf<List<EmpresaTermometroDto>>(emptyList()) }
    if (dados != null && dados.empresas.isNotEmpty()) empresas = dados.empresas

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTopBar(title = "Termômetro", userName = user.displayName, onAccount = onAccount, onBack = onBack)

        SelectorsRow {
            EmpresaSelector(
                empresas = empresas.filter { fixedEmpresaId == null || it.id == fixedEmpresaId }.map { EmpresaOption(it.id, it.nome, Classificacao.fromWire(it.ultima?.classificacao)) },
                selecionadaId = empresaId ?: dados?.empresaId,
                onSelect = { empresaId = it },
                modifier = it,
            )
            PeriodSelectorChip(
                label = dados?.periodo?.let { formatPeriodShort(it.inicio, it.fim) } ?: periodLabel ?: "Últimos 30 dias",
                enabled = true,
                modifier = it,
                onClick = { periodoSheet = true },
            )
        }

        mensagem?.let { InfoNote("Pronto", it) }
        LoadStateContent(state, onRetry = loader::reload, onSignIn = onSignIn) { d ->
            val podeEditar = d.podeEditar && user.canWrite
            val podePublicar = d.podePublicar && user.isAdminOrManager
            PainelContent(
                dados = d,
                podeEditar = podeEditar,
                podePublicar = podePublicar,
                onNova = { sheet = null to AvaliacaoForm(d.empresaId ?: 0, periodoInicio = d.periodo?.inicio, periodoFim = d.periodo?.fim) },
                onEditar = { a -> sheet = a.id to AvaliacaoForm.from(a, d.empresaId ?: 0) },
                // Publicada não se edita: a correção vira um NOVO rascunho com o mesmo conteúdo.
                onCorrigir = { a -> sheet = null to AvaliacaoForm.from(a, d.empresaId ?: 0) },
                onPublicar = { a -> confirmarPublicar = a.id },
            )
        }
    }

    if (periodoSheet) {
        TeamPeriodSheet(
            selected = desde?.let { d -> ate?.let { a -> PeriodRange(periodLabel.orEmpty(), d, a) } },
            onSelect = { desde = it.desde; ate = it.ate; periodLabel = it.label; periodoSheet = false },
            onDismiss = { periodoSheet = false },
        )
    }

    val atual = (state as? LoadState.Loaded)?.data
    sheet?.let { (id, form) ->
        AvaliacaoSheet(
            titulo = if (id == null) "Nova avaliação" else "Editar rascunho",
            initial = form,
            aplicacoes = atual?.aplicacoes.orEmpty(),
            onDismiss = { sheet = null },
            onSave = { f ->
                val r = repository.salvarAvaliacao(id, f.toRequest())
                if (r is ApiResult.Success) { sheet = null; mensagem = "Rascunho salvo. O cliente só vê depois de publicar."; loader.reload() }
                r
            },
        )
    }
    confirmarPublicar?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmarPublicar = null },
            title = { Text("Publicar avaliação?") },
            text = { Text("O cliente passa a ver esta avaliação e os números do período ficam congelados. Depois de publicada ela não pode ser editada (só corrigida com um novo rascunho).") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarPublicar = null
                    scope.launch {
                        when (val r = repository.publicar(id)) {
                            is ApiResult.Success -> { mensagem = "Avaliação publicada."; loader.reload() }
                            is ApiResult.Error -> mensagem = r.message
                        }
                    }
                }) { Text("Publicar") }
            },
            dismissButton = { TextButton(onClick = { confirmarPublicar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PainelContent(
    dados: TermometroEquipeDto,
    podeEditar: Boolean,
    podePublicar: Boolean,
    onNova: () -> Unit,
    onEditar: (AvaliacaoEquipeDto) -> Unit,
    onCorrigir: (AvaliacaoEquipeDto) -> Unit,
    onPublicar: (AvaliacaoEquipeDto) -> Unit,
) {
    if (dados.empresas.isEmpty()) {
        EmptyDashboardState("Nenhuma empresa ativa", "Quando houver empresas ativas, o painel aparece aqui.")
        return
    }
    val periodo = dados.periodo?.let { formatPeriod(it.inicio, it.fim) } ?: "Período"

    // Card principal: a avaliação publicada mais recente. Rascunho não é avaliação para o cliente.
    val publicada = dados.avaliacoes.firstOrNull { it.status == "publicada" }
    val rascunhos = dados.avaliacoes.count { it.status != "publicada" }
    EvaluationCard(
        eyebrow = "Avaliação publicada",
        classificacao = Classificacao.fromWire(publicada?.classificacao),
        periodo = publicada?.let { formatPeriod(it.periodoInicio, it.periodoFim) },
        justificativa = publicada?.justificativa,
        feedback = publicada?.feedback,
        rodape = when {
            publicada?.publicadaEm != null -> "Publicado em ${formatDate(publicada.publicadaEm)}"
            rascunhos > 0 -> "Há $rascunhos rascunho(s) ainda não publicado(s)."
            else -> null
        },
    )

    if (dados.truncado) {
        InfoNote("Números parciais", "O período tem leads demais: os números mostram só uma parte. Reduza o período.")
    }
    dados.resultados?.let { ResultadosSection(it, periodo, titulo = "Resultados ao vivo do período") }
    dados.aplicacoes.forEach { MetasSection("Metas — ${it.nome}" + (it.metodoNome?.let { m -> " ($m)" } ?: ""), it.metas) }

    AvaliacoesCard(dados.avaliacoes, podeEditar, podePublicar, onNova, onEditar, onCorrigir, onPublicar)
    if (!podeEditar) {
        InfoNote("Somente leitura", "Seu papel permite acompanhar, mas não gravar avaliações.")
    }
}

@Composable
private fun AvaliacoesCard(
    avaliacoes: List<AvaliacaoEquipeDto>,
    podeEditar: Boolean,
    podePublicar: Boolean,
    onNova: () -> Unit,
    onEditar: (AvaliacaoEquipeDto) -> Unit,
    onCorrigir: (AvaliacaoEquipeDto) -> Unit,
    onPublicar: (AvaliacaoEquipeDto) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Avaliações", eyebrow = "Todas as avaliações")
        if (podeEditar) Button(onClick = onNova, modifier = Modifier.fillMaxWidth()) { Text("Nova avaliação") }
        if (avaliacoes.isEmpty()) {
            EmptyDashboardState("Ainda não avaliado", "Esta empresa ainda não tem avaliações. Elas aparecem aqui como rascunho até serem publicadas.")
            return
        }
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                avaliacoes.forEachIndexed { index, a ->
                    val c = Classificacao.fromWire(a.classificacao)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClassificationDot(c)
                            Text(formatPeriod(a.periodoInicio, a.periodoFim), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (a.status == "publicada") StatusBadge("Publicada", BadgeTone.Success) else StatusBadge("Rascunho", BadgeTone.Warning)
                        }
                        Text(c.label, style = MaterialTheme.typography.labelMedium, color = c.color())
                        a.justificativa?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        a.notaInterna?.takeIf { it.isNotBlank() }?.let {
                            Text("Nota interna: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (podeEditar || podePublicar) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (a.status != "publicada") {
                                    if (podeEditar) OutlinedButton(onClick = { onEditar(a) }) { Text("Editar") }
                                    if (podePublicar) Button(onClick = { onPublicar(a) }) { Text("Publicar") }
                                } else if (podeEditar) {
                                    OutlinedButton(onClick = { onCorrigir(a) }) { Text("Corrigir (novo rascunho)") }
                                }
                            }
                        }
                    }
                    if (index != avaliacoes.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
