package br.com.synergyone.android.ui.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.lead.LeadRepository
import br.com.synergyone.android.ui.common.ErrorView
import br.com.synergyone.android.ui.common.LoadingView
import br.com.synergyone.android.ui.common.origemLabel
import br.com.synergyone.android.ui.common.etapaLabel
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.common.formatMoney
import br.com.synergyone.android.ui.common.initialsOf
import br.com.synergyone.android.ui.components.BadgeTone
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.StatusBadge
import br.com.synergyone.android.ui.components.SynergyCard
import br.com.synergyone.android.ui.termometro.EmpresaOption
import br.com.synergyone.android.ui.termometro.EmpresaSelector
import br.com.synergyone.android.ui.theme.synergy

// "captado" não é filtrável na API (lead sem jornada aparece como captado), então não vira chip.
private val filtros = listOf("qualificado", "contato", "proposta", "negociacao", "ganho", "perdido")

@Composable
fun LeadsScreen(
    repository: LeadRepository,
    userName: String,
    empresas: List<EmpresaOption>,
    onAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    /** Abre a lista já filtrada por uma empresa (a partir da ficha); esconde o seletor de empresa. */
    fixedEmpresaId: Long? = null,
    withHeader: Boolean = true,
    title: String = "Leads",
    onBack: (() -> Unit)? = null,
    /** Quando informado (equipe), o cartão do lead abre o detalhe. Cliente: somente leitura. */
    onLeadClick: ((Long) -> Unit)? = null,
) {
    val viewModel: LeadsViewModel = viewModel(key = "leads-$fixedEmpresaId", factory = LeadsViewModel.Factory(repository, fixedEmpresaId))
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (withHeader) item { AppTopBar(title = title, userName = userName, onAccount = onAccount, onBack = onBack) }
        if (empresas.size > 1 && fixedEmpresaId == null) {
            item {
                EmpresaSelector(
                    empresas = empresas,
                    selecionadaId = state.empresaId,
                    onSelect = viewModel::selectEmpresa,
                    todasLabel = "Todas as empresas",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Text(
                if (state.isLoading && state.leads.isEmpty()) "Carregando…" else "${state.total} no total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = state.etapa == null, onClick = { viewModel.selectEtapa(null) }, colors = br.com.synergyone.android.ui.components.synergyChipColors(), label = { Text("Todos") })
                filtros.forEach { etapa ->
                    FilterChip(selected = state.etapa == etapa, onClick = { viewModel.selectEtapa(etapa) }, colors = br.com.synergyone.android.ui.components.synergyChipColors(), label = { Text(etapaLabel(etapa)) })
                }
            }
        }

        when {
            state.isLoading -> item { LoadingView(Modifier.padding(top = 4.dp)) }
            state.error != null && state.leads.isEmpty() -> item {
                ErrorView(state.error!!, state.sessionExpired, onRetry = viewModel::reload, onSignIn = onSignIn)
            }
            state.leads.isEmpty() -> item {
                EmptyDashboardState(
                    "Nenhum lead por aqui",
                    if (state.etapa == null) "Quando um lead for captado, ele aparece nesta lista."
                    else "Nenhum lead na etapa ${etapaLabel(state.etapa)}.",
                )
            }
            else -> {
                items(state.leads, key = { it.id }) { LeadCard(it, onLeadClick) }
                item {
                    LoadMoreFooter(
                        loading = state.isLoadingMore,
                        canLoadMore = state.canLoadMore,
                        error = state.error,
                        onLoadMore = viewModel::loadMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadMoreFooter(loading: Boolean, canLoadMore: Boolean, error: String?, onLoadMore: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            canLoadMore -> OutlinedButton(onClick = onLoadMore) { Text("Carregar mais") }
        }
    }
}

internal fun etapaTone(etapa: String) = when (etapa) {
    "qualificado", "contato" -> BadgeTone.Info
    "proposta" -> BadgeTone.Brand
    "negociacao" -> BadgeTone.Warning
    "ganho" -> BadgeTone.Success
    "perdido" -> BadgeTone.Danger
    else -> BadgeTone.Neutral
}

@Composable
private fun LeadCard(lead: LeadDto, onClick: ((Long) -> Unit)?) {
    SynergyCard(modifier = Modifier.fillMaxWidth().let { m -> if (onClick != null) m.clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Abrir lead") { onClick(lead.id) } else m }) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialsOf(lead.nome.orEmpty()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        lead.nome?.takeIf { it.isNotBlank() } ?: "Lead sem nome",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(etapaLabel(lead.etapa), etapaTone(lead.etapa))
                }
                val origem = listOfNotNull(origemLabel(lead.origem, lead.canal), lead.cidade?.takeIf { it.isNotBlank() }).joinToString(" • ")
                if (origem.isNotBlank()) Text(origem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                lead.mensagem?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(lead.capturadoEm), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    lead.vendaCentavos?.let {
                        Text(formatMoney(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.synergy.success)
                    }
                    if (lead.entradas > 1) {
                        Text("Entrou ${lead.entradas}×", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
