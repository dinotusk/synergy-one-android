package br.com.synergyone.android.ui.leads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.ApiErrors
import br.com.synergyone.android.data.api.JornadaDetalheDto
import br.com.synergyone.android.data.api.JornadaLeadDto
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.jornada.FormError
import br.com.synergyone.android.data.jornada.JornadaForm
import br.com.synergyone.android.data.jornada.JornadaLocalizada
import br.com.synergyone.android.data.jornada.parseMoneyInput
import br.com.synergyone.android.di.AppContainer
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.common.LoadState
import br.com.synergyone.android.ui.common.LoadStateContent
import br.com.synergyone.android.ui.common.etapaLabel
import br.com.synergyone.android.ui.common.etapas
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.common.formatMoney
import br.com.synergyone.android.ui.common.origemLabel
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.SectionTitle
import br.com.synergyone.android.ui.components.StatusBadge
import br.com.synergyone.android.ui.components.SynergyCard
import br.com.synergyone.android.ui.navigation.AppNavigator
import br.com.synergyone.android.ui.theme.synergy
import kotlinx.coroutines.launch

/**
 * Detalhe de um lead para a **equipe**: contato, estado da jornada, notas, recebimentos e histórico.
 * Gravar exige `podeEditar` (do servidor) e papel diferente de `leitor`; o servidor continua sendo
 * quem decide (403 `sem_permissao` aparece como mensagem, sem deslogar).
 */
@Composable
fun LeadDetailScreen(
    leadId: Long,
    user: AuthenticatedUser,
    container: AppContainer,
    nav: AppNavigator,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lead = remember(leadId) { container.leadRepository.cached(leadId) }
    if (lead == null) {
        Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppTopBar("Lead", user.displayName, nav::openPerfil, onBack = nav::back)
            EmptyDashboardState("Abra o lead pela lista", "Este lead não está mais carregado. Volte à lista e toque nele de novo.")
        }
        return
    }

    val repo = container.jornadaRepository
    val estadoLoader = rememberLoader("lead-estado-$leadId") { repo.estadoDoLead(leadId, lead.empresaId ?: 0, lead.capturadoEm) }
    val detalheLoader = rememberLoader("lead-detalhe-$leadId") { repo.detalhe(leadId) }
    val estado by estadoLoader.state.collectAsState()
    val detalhe by detalheLoader.state.collectAsState()
    val scope = rememberCoroutineScope()

    var sheet by rememberSaveable { mutableStateOf<String?>(null) } // "jornada" | "recebimento"
    var mensagem by remember { mutableStateOf<String?>(null) }

    fun recarregar() { estadoLoader.reload(); detalheLoader.reload() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTopBar(title = lead.nome?.takeIf { it.isNotBlank() } ?: "Lead", userName = user.displayName, onAccount = nav::openPerfil, onBack = nav::back)
        LeadCabecalho(lead)

        val localizada = (estado as? LoadState.Loaded)?.data
        val podeEditar = user.canWrite && localizada?.apoio?.podeEditar == true

        mensagem?.let { InfoNote("Pronto", it) }

        LoadStateContent(estado, onRetry = estadoLoader::reload, onSignIn = onSignIn) { loc ->
            if (loc == null) {
                InfoNote("Jornada indisponível", "Não foi possível localizar o estado da jornada deste lead (ele pode estar oculto por ser repetido ou teste). Você vê o contato e o histórico, sem editar.")
            } else {
                JornadaCard(loc, podeEditar, onEdit = { sheet = "jornada" })
            }
        }

        LoadStateContent(detalhe, onRetry = detalheLoader::reload, onSignIn = onSignIn) { d ->
            RecebimentosCard(d, localizada?.lead, podeEditar, onAdd = { sheet = "recebimento" })
            NotasCard(d, podeEditar) { texto ->
                scope.launch {
                    when (val r = repo.adicionarNota(leadId, texto)) {
                        is ApiResult.Success -> { mensagem = "Nota adicionada."; detalheLoader.reload() }
                        is ApiResult.Error -> mensagem = r.message
                    }
                }
            }
            HistoricoCard(d)
        }
    }

    val loc = (estado as? LoadState.Loaded)?.data
    if (sheet == "jornada" && loc != null) {
        EditJornadaSheet(
            lead = loc.lead,
            localizada = loc,
            onDismiss = { sheet = null },
            onSave = { form ->
                val r = repo.salvar(leadId, form.toRequest())
                if (r is ApiResult.Success) { sheet = null; mensagem = "Jornada atualizada."; recarregar() }
                r
            },
        )
    }
    if (sheet == "recebimento") {
        RecebimentoSheet(
            onDismiss = { sheet = null },
            onSave = { valor, data ->
                val r = repo.registrarRecebimento(leadId, valor, data)
                if (r is ApiResult.Success) { sheet = null; mensagem = "Recebimento registrado."; detalheLoader.reload(); estadoLoader.reload() }
                r
            },
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LeadCabecalho(lead: LeadDto) {
    val context = LocalContext.current
    SynergyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusBadge(etapaLabel(lead.etapa), etapaTone(lead.etapa))
                if (lead.entradas > 1) Text("Entrou ${lead.entradas}×", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val origem = listOfNotNull(origemLabel(lead.origem, lead.canal), lead.cidade?.takeIf { it.isNotBlank() }).joinToString(" • ")
            if (origem.isNotBlank()) Text(origem, style = MaterialTheme.typography.bodyMedium)
            Text("Captado em ${formatDate(lead.capturadoEm)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            lead.mensagem?.takeIf { it.isNotBlank() }?.let { Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            lead.email?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            lead.telefone?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lead.telefone?.takeIf { it.isNotBlank() }?.let { tel ->
                    OutlinedButton(onClick = { ContactActions.dial(context, tel) }) { Icon(Icons.Filled.Call, null, Modifier.padding(end = 6.dp)); Text("Ligar") }
                    OutlinedButton(onClick = { ContactActions.whatsapp(context, tel) }) { Icon(Icons.Filled.Chat, null, Modifier.padding(end = 6.dp)); Text("WhatsApp") }
                }
                lead.email?.takeIf { it.isNotBlank() }?.let { mail ->
                    OutlinedButton(onClick = { ContactActions.email(context, mail) }) { Icon(Icons.Filled.Email, null, Modifier.padding(end = 6.dp)); Text("E-mail") }
                }
            }
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String?) {
    if (valor.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun JornadaCard(loc: JornadaLocalizada, podeEditar: Boolean, onEdit: () -> Unit) {
    val j = loc.lead
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Jornada", eyebrow = "Vendas")
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Linha("Etapa", etapaLabel(j.etapa))
                Linha("Responsável", loc.apoio.responsaveis.firstOrNull { it.id == j.responsavelId }?.nome)
                Linha("Campanha", loc.apoio.campanhas.firstOrNull { it.id == j.campanhaId }?.nome)
                Linha("Proposta", j.propostaCentavos?.let { formatMoney(it) + (j.propostaEnviadaEm?.let { d -> " • ${formatDate(d)}" } ?: "") })
                Linha("Venda", j.vendaCentavos?.let { formatMoney(it) + (j.vendaFechadaEm?.let { d -> " • ${formatDate(d)}" } ?: "") })
                Linha("Recebido", j.recebidoCentavos?.takeIf { it > 0 }?.let { formatMoney(it) })
                Linha("Motivo da perda", j.motivoPerda)
                if (podeEditar) {
                    Button(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Atualizar jornada") }
                }
            }
        }
        if (!podeEditar) Text("Somente leitura: seu papel não permite alterar a jornada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecebimentosCard(d: JornadaDetalheDto, jornada: JornadaLeadDto?, podeEditar: Boolean, onAdd: () -> Unit) {
    val ganho = jornada?.etapa == "ganho"
    if (d.recebimentos.isEmpty() && !(podeEditar && ganho)) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Recebimentos", eyebrow = "Financeiro")
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                if (d.recebimentos.isEmpty()) Text("Nenhum recebimento registrado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                d.recebimentos.forEach { Linha(formatDate(it.recebidoEm), formatMoney(it.centavos)) }
                if (podeEditar && ganho) OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Registrar recebimento") }
            }
        }
    }
}

@Composable
private fun NotasCard(d: JornadaDetalheDto, podeEditar: Boolean, onAdd: (String) -> Unit) {
    var texto by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Notas internas", eyebrow = "Somente equipe")
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (podeEditar) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { if (it.length <= 2000) texto = it },
                        label = { Text("Nova nota") },
                        supportingText = { Text("${texto.length}/2000") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Button(onClick = { onAdd(texto); texto = "" }, enabled = texto.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Adicionar nota") }
                    if (d.notas.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (d.notas.isEmpty() && !podeEditar) Text("Nenhuma nota.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                d.notas.forEach { n ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(n.texto, style = MaterialTheme.typography.bodyMedium)
                        Text(listOfNotNull(n.autor, formatDate(n.criadaEm).takeIf { it != "—" }).joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricoCard(d: JornadaDetalheDto) {
    if (d.historico.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Histórico", eyebrow = "Mudanças de etapa")
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                d.historico.forEachIndexed { i, h ->
                    Column(modifier = Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (h.etapaDe != null && h.etapaDe != h.etapaPara) "${etapaLabel(h.etapaDe)} → ${etapaLabel(h.etapaPara)}" else etapaLabel(h.etapaPara),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(listOfNotNull(h.autor, formatDate(h.em).takeIf { it != "—" }).joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (i != d.historico.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditJornadaSheet(
    lead: JornadaLeadDto,
    localizada: JornadaLocalizada,
    onDismiss: () -> Unit,
    onSave: suspend (JornadaForm) -> ApiResult<Unit>,
) {
    var form by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.listSaver(
        save = { f -> listOf(f.etapa, f.responsavelId, f.campanhaId, f.propostaValor, f.propostaEnviadaEm, f.vendaValor, f.vendaFechadaEm, f.motivoPerda, f.perdidoEm, f.clienteEmpresaId) },
        restore = { l -> JornadaForm(l[0] as String, l[1] as String?, l[2] as Long?, l[3] as String, l[4] as String?, l[5] as String, l[6] as String?, l[7] as String, l[8] as String?, l[9] as Long?) },
    )) { mutableStateOf(JornadaForm.from(lead)) }
    var error by remember { mutableStateOf<FormError?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun erro(campo: String) = error?.takeIf { it.campo == campo }?.message

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Atualizar jornada", style = MaterialTheme.typography.titleLarge)
            Text("Etapa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                etapas.forEach { e -> FilterChip(selected = form.etapa == e, onClick = { form = form.copy(etapa = e); error = null }, label = { Text(etapaLabel(e)) }) }
            }
            PickerField("Responsável", localizada.apoio.responsaveis.map { it.id to it.nome }, form.responsavelId, "Sem responsável") { form = form.copy(responsavelId = it) }
            PickerField(
                "Campanha",
                localizada.apoio.campanhas.filter { it.empresaId == null || it.empresaId == lead.empresaId }.map { it.id to it.nome },
                form.campanhaId, "Sem campanha",
            ) { form = form.copy(campanhaId = it) }

            MoneyField("Valor da proposta", form.propostaValor, { form = form.copy(propostaValor = it) }, erro("propostaValor"))
            DateField("Proposta enviada em", form.propostaEnviadaEm, { form = form.copy(propostaEnviadaEm = it) })

            if (form.etapa == "ganho") {
                MoneyField("Valor da venda", form.vendaValor, { form = form.copy(vendaValor = it) }, erro("vendaValor"))
                DateField("Fechada em", form.vendaFechadaEm, { form = form.copy(vendaFechadaEm = it) }, erro("vendaFechadaEm"))
            }
            if (form.etapa == "perdido") {
                OutlinedTextField(
                    value = form.motivoPerda,
                    onValueChange = { if (it.length <= 300) form = form.copy(motivoPerda = it) },
                    label = { Text("Motivo da perda") },
                    supportingText = { Text(erro("motivoPerda") ?: "${form.motivoPerda.length}/300") },
                    isError = erro("motivoPerda") != null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                DateField("Perdido em", form.perdidoEm, { form = form.copy(perdidoEm = it) }, erro("perdidoEm"))
            }

            error?.takeIf { it.campo == "_servidor" }?.let { Text(it.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

            Button(
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val local = form.validate()
                    if (local != null) { error = local; return@Button }
                    saving = true; error = null
                    scope.launch {
                        val r = onSave(form)
                        saving = false
                        if (r is ApiResult.Error) error = FormError(r.field ?: "_servidor", r.message).let { e ->
                            // Campo desconhecido da tela: mostra a mensagem do servidor no rodapé.
                            if (e.campo in setOf("propostaValor", "vendaValor", "vendaFechadaEm", "motivoPerda", "perdidoEm")) e else e.copy(campo = "_servidor")
                        }
                    }
                },
            ) {
                if (saving) CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                Text(if (saving) "Salvando…" else "Salvar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecebimentoSheet(onDismiss: () -> Unit, onSave: suspend (valor: String, data: String) -> ApiResult<Unit>) {
    var valor by rememberSaveable { mutableStateOf("") }
    var data by rememberSaveable { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Registrar recebimento", style = MaterialTheme.typography.titleLarge)
            MoneyField("Valor recebido", valor, { valor = it })
            DateField("Recebido em", data, { data = it })
            erro?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val v = parseMoneyInput(valor)
                    when {
                        v == null || v.toBigDecimal().signum() <= 0 -> erro = "Informe um valor maior que zero."
                        data.isNullOrBlank() -> erro = "Informe a data do recebimento."
                        else -> { saving = true; erro = null; scope.launch { val r = onSave(v, data!!); saving = false; if (r is ApiResult.Error) erro = r.message } }
                    }
                },
            ) { Text(if (saving) "Salvando…" else "Registrar") }
        }
    }
}
