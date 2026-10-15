package br.com.synergyone.android.ui.termometro

import br.com.synergyone.android.ui.theme.synergy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.api.AvaliacaoPublicadaDto
import br.com.synergyone.android.data.api.TermometroClienteDto
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.termometro.TermometroRepository
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.common.LoadState
import br.com.synergyone.android.ui.common.LoadStateContent
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.common.formatPeriod
import br.com.synergyone.android.ui.common.formatPeriodShort
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.SectionTitle
import br.com.synergyone.android.ui.components.SynergyCard

/**
 * Termômetro como o cliente vê no portal: só avaliações **publicadas**, somente leitura. O
 * "período" é a avaliação publicada que ele quer rever (atual ou histórico). Sem avaliação,
 * estado neutro "Ainda não avaliado".
 */
@Composable
fun TermometroClienteScreen(
    repository: TermometroRepository,
    user: AuthenticatedUser,
    onAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    /** Abre já numa empresa (vindo da ficha): esconde a troca de empresa e mostra "voltar". */
    fixedEmpresaId: Long? = null,
    onBack: (() -> Unit)? = null,
) {
    var empresaId by rememberSaveable { mutableStateOf<Long?>(fixedEmpresaId) }
    var avaliacaoId by rememberSaveable(empresaId) { mutableStateOf<Long?>(null) }
    var sheet by rememberSaveable { mutableStateOf(false) }

    val loader = rememberLoader("termometro-publicado-$empresaId") { repository.publicado(empresaId) }
    val state by loader.state.collectAsState()
    val dados = (state as? LoadState.Loaded)?.data

    val avaliacoes = dados?.let { d -> (listOfNotNull(d.atual) + d.historico).distinctBy { it.id } }.orEmpty()
    val selecionada = avaliacoes.firstOrNull { it.id == avaliacaoId } ?: dados?.atual

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTopBar(title = "Termômetro", userName = user.displayName, onAccount = onAccount, onBack = onBack)

        SelectorsRow {
            EmpresaSelector(
                empresas = user.empresas.filter { fixedEmpresaId == null || it.id == fixedEmpresaId }.map { EmpresaOption(it.id, it.nome) },
                selecionadaId = empresaId ?: dados?.empresa?.id,
                onSelect = { empresaId = it },
                modifier = it,
            )
            PeriodSelectorChip(
                label = selecionada?.let { formatPeriodShort(it.periodoInicio, it.periodoFim) } ?: "Sem avaliações",
                enabled = avaliacoes.isNotEmpty(),
                modifier = it,
                onClick = { sheet = true },
            )
        }

        LoadStateContent(state, onRetry = loader::reload, onSignIn = onSignIn) { d ->
            PublicadoContent(d, selecionada, onSelect = { avaliacaoId = it })
        }
    }

    if (sheet && avaliacoes.isNotEmpty()) {
        AvaliacaoPeriodSheet(avaliacoes, selecionada?.id, onSelect = { avaliacaoId = it; sheet = false }, onDismiss = { sheet = false })
    }
}

/** Empresa e período lado a lado; em fonte ampliada, um sobre o outro. */
@Composable
fun SelectorsRow(content: @Composable (Modifier) -> Unit) {
    if (LocalConfiguration.current.fontScale > 1.15f) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content(Modifier.fillMaxWidth()) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val half = Modifier.weight(1f)
            content(half)
        }
    }
}

@Composable
private fun PublicadoContent(dados: TermometroClienteDto, selecionada: AvaliacaoPublicadaDto?, onSelect: (Long) -> Unit) {
    val classificacao = Classificacao.fromWire(selecionada?.classificacao)
    EvaluationCard(
        eyebrow = if (selecionada != null && selecionada.id == dados.atual?.id) "Avaliação publicada" else "Avaliação anterior",
        classificacao = classificacao,
        periodo = selecionada?.let { formatPeriod(it.periodoInicio, it.periodoFim) },
        justificativa = selecionada?.justificativa,
        feedback = selecionada?.feedback,
        rodape = selecionada?.publicadaEm?.let { "Publicado em ${formatDate(it)}" },
    )
    if (selecionada == null) return

    val periodo = formatPeriod(selecionada.periodoInicio, selecionada.periodoFim)
    val resultados = selecionada.resultados
    if (resultados != null) {
        ResultadosSection(resultados, periodo)
        resultados.aplicacao?.let { MetasSection("Metas — ${it.nome}", it.metas) }
    } else {
        InfoNote("Resultados indisponíveis", "Esta avaliação não guardou os números do período.")
    }

    if (dados.historico.size > 1) HistoricoCard(dados.historico, selecionada.id, onSelect)
}

@Composable
private fun HistoricoCard(historico: List<AvaliacaoPublicadaDto>, selecionadaId: Long, onSelect: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Histórico", eyebrow = "Avaliações publicadas")
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                historico.forEachIndexed { index, item ->
                    val c = Classificacao.fromWire(item.classificacao)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Ver este período") { onSelect(item.id) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ClassificationDot(c)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(formatPeriod(item.periodoInicio, item.periodoFim), style = MaterialTheme.typography.bodyMedium)
                            Text(c.label, style = MaterialTheme.typography.labelMedium, color = c.color())
                        }
                        if (item.id == selecionadaId) Text("Em exibição", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.synergy.primaryText)
                    }
                    if (index != historico.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
