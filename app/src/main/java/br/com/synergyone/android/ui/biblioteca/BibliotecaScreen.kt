package br.com.synergyone.android.ui.biblioteca

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.synergyone.android.data.api.MaterialDto
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.biblioteca.AREAS_BIBLIOTECA
import br.com.synergyone.android.data.biblioteca.BibliotecaRepository
import br.com.synergyone.android.data.biblioteca.linkSeguro
import br.com.synergyone.android.ui.common.ErrorView
import br.com.synergyone.android.ui.common.LoadingView
import br.com.synergyone.android.ui.common.PullRefreshBox
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.BadgeTone
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.StatusBadge
import br.com.synergyone.android.ui.components.SynergyCard
import br.com.synergyone.android.ui.components.synergyChipColors
import br.com.synergyone.android.ui.modules.WebLauncher
import br.com.synergyone.android.ui.termometro.EmpresaOption
import br.com.synergyone.android.ui.termometro.EmpresaSelector
import br.com.synergyone.android.data.auth.model.UserRole

/**
 * Biblioteca: materiais da empresa, **somente leitura** (cadastrar e liberar continua no site). O cliente só recebe o que foi
 * liberado a ele; a equipe vê também o uso interno, marcado. Cada material abre o link externo cadastrado (HTTPS) no navegador.
 */
@Composable
fun BibliotecaScreen(
    repository: BibliotecaRepository,
    user: AuthenticatedUser,
    onAccount: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: BibliotecaViewModel = viewModel(key = "biblioteca-${user.id}", factory = BibliotecaViewModel.Factory(repository))
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val equipe = user.role == UserRole.TEAM

    PullRefreshBox(refreshing = state.isRefreshing, failed = state.refreshFailed, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { AppTopBar(title = "Biblioteca", userName = user.displayName, onAccount = onAccount, onBack = onBack) }
            if (!equipe && user.empresas.size > 1) {
                item {
                    EmpresaSelector(
                        empresas = user.empresas.map { EmpresaOption(it.id, it.nome) },
                        selecionadaId = state.empresaId,
                        onSelect = viewModel::selectEmpresa,
                        todasLabel = "Todas as empresas",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Text(
                    if (state.isLoading && state.itens.isEmpty()) "Carregando…" else "${state.total} ${if (state.total == 1) "material" else "materiais"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.area == null, onClick = { viewModel.selectArea(null) }, colors = synergyChipColors(), label = { Text("Todas") })
                    AREAS_BIBLIOTECA.forEach { a ->
                        FilterChip(selected = state.area == a, onClick = { viewModel.selectArea(a) }, colors = synergyChipColors(), label = { Text(a) })
                    }
                }
            }
            when {
                state.isLoading -> item { LoadingView(Modifier.padding(top = 4.dp)) }
                state.error != null && state.itens.isEmpty() -> item { ErrorView(state.error!!, state.sessionExpired, onRetry = viewModel::reload, onSignIn = onSignIn) }
                state.itens.isEmpty() -> item {
                    EmptyDashboardState(
                        if (state.area == null) "Nenhum material liberado" else "Nenhum material nesta área",
                        when {
                            state.area != null -> "Não há material liberado nesta área. Escolha outra área ou \"Todas\"."
                            equipe -> "Cadastre e libere materiais pelo site; eles aparecem aqui."
                            else -> "Quando a equipe liberar materiais para a sua empresa, eles aparecem aqui."
                        },
                    )
                }
                else -> {
                    items(state.itens, key = { it.id }) { MaterialCard(it, equipe) { link -> WebLauncher.openAbsolute(context, link) } }
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            when {
                                state.isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                state.canLoadMore -> OutlinedButton(onClick = viewModel::loadMore) { Text("Carregar mais") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(item: MaterialDto, equipe: Boolean, onOpen: (String) -> Unit) {
    val link = linkSeguro(item.url)
    val base = Modifier.fillMaxWidth()
    SynergyCard(modifier = if (link != null) base.clickable(role = Role.Button, onClickLabel = "Abrir material no navegador") { onOpen(link) } else base) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.titulo.ifBlank { "Material sem título" }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                item.tipo?.takeIf { it.isNotBlank() }?.let { StatusBadge(it, BadgeTone.Neutral) }
            }
            item.descricao?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            val meta = listOfNotNull(item.area?.takeIf { it.isNotBlank() }, item.atualizadoEm?.let { "Atualizado em ${formatDate(it)}" }).joinToString(" • ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (equipe) {
                // Só a equipe recebe estes campos; o cliente nunca vê uso interno.
                val destino = when {
                    item.empresaNome == null && item.empresaId == null -> "Uso interno"
                    else -> item.empresaNome ?: "Empresa ${item.empresaId}"
                }
                val liberado = when (item.visivelCliente) { true -> "Liberado ao cliente"; false -> "Não liberado ao cliente"; null -> null }
                Text(listOfNotNull(destino, liberado).joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (link != null) "Abre um link externo no navegador" else "Sem link disponível",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
