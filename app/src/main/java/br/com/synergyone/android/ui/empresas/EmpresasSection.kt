package br.com.synergyone.android.ui.empresas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import br.com.synergyone.android.ui.common.LoadStateContent
import br.com.synergyone.android.ui.common.PullRefresh
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.data.empresa.Empresa
import br.com.synergyone.android.data.empresa.EmpresaRepository
import br.com.synergyone.android.ui.common.moduloLabel
import br.com.synergyone.android.ui.components.BadgeTone
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.StatusBadge
import br.com.synergyone.android.ui.components.SynergyCard

/** Lista as empresas que a conta pode ver (a RLS decide: equipe vê todas, cliente só as suas). */
@Composable
fun EmpresasSection(
    repository: EmpresaRepository,
    emptyTitle: String,
    emptyDetail: String,
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit,
    onEmpresaClick: ((Long) -> Unit)? = null,
) {
    val loader = rememberLoader("empresas") { repository.listar() }
    val state by loader.state.collectAsState()

    PullRefresh(listOf(loader), modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoadStateContent(state, onRetry = loader::reload, onSignIn = onSignIn) { empresas ->
                if (empresas.isEmpty()) EmptyDashboardState(emptyTitle, emptyDetail)
                else empresas.forEach { EmpresaCard(it, onEmpresaClick) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmpresaCard(empresa: Empresa, onClick: ((Long) -> Unit)?) {
    SynergyCard(modifier = Modifier.fillMaxWidth().let { m -> if (onClick != null) m.clickable(onClickLabel = "Abrir ficha da empresa") { onClick(empresa.id) } else m }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    empresa.nome,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                empresa.status?.let { StatusBadge(statusLabel(it), statusTone(it)) }
            }
            empresa.plano?.let {
                Text(
                    "Plano $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (empresa.modulosAtivos.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    empresa.modulosAtivos.forEach { StatusBadge(moduloLabel(it), BadgeTone.Brand) }
                }
            }
        }
    }
}

/** Mapeia o texto livre de `status` para um tom; desconhecido fica neutro (nunca vermelho por padrão). */
internal fun statusTone(status: String) = when (status.lowercase()) {
    "ativo", "ativa", "ok" -> BadgeTone.Success
    "atencao", "atenção", "pendente" -> BadgeTone.Warning
    "inativo", "inativa", "cancelado", "gargalo" -> BadgeTone.Danger
    else -> BadgeTone.Neutral
}

/** Texto livre do servidor com a acentuação correta; valores desconhecidos aparecem como vieram. */
internal fun statusLabel(status: String) = when (status.lowercase()) {
    "atencao" -> "atenção"
    else -> status
}
