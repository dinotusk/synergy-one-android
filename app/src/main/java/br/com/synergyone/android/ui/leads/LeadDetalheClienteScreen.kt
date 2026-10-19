package br.com.synergyone.android.ui.leads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.lead.LeadRepository
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.common.formatMoney
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.SynergyCard

/**
 * Detalhe do lead para a conta de **cliente**, somente leitura. Mostra só o que `GET /leads` já entrega
 * (contato, mensagem, origem, etapa, venda). Histórico, notas e recebimentos são rotas da equipe (prefixo `/equipe`):
 * o cliente não tem permissão e o app não as chama. Não há nenhuma ação de edição.
 */
@Composable
fun LeadDetalheClienteScreen(
    leadId: Long,
    user: AuthenticatedUser,
    repository: LeadRepository,
    onAccount: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lead = remember(leadId) { repository.cached(leadId) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTopBar(title = lead?.nome?.takeIf { it.isNotBlank() } ?: "Lead", userName = user.displayName, onAccount = onAccount, onBack = onBack)
        if (lead == null) {
            EmptyDashboardState("Abra o lead pela lista", "Este lead não está mais carregado. Volte à lista e toque nele de novo.")
            return@Column
        }
        LeadCabecalho(lead)
        if (lead.etapa == "ganho") {
            SynergyCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Text("Venda", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    Linha("Valor", lead.vendaCentavos?.takeIf { it > 0 }?.let(::formatMoney) ?: "Não informado")
                    Linha("Fechada em", lead.vendaFechadaEm?.takeIf { it.isNotBlank() }?.let(::formatDate) ?: "Data não informada")
                }
            }
        }
        InfoNote(
            "Somente leitura",
            "Você vê os dados do lead e a etapa atual. O histórico, as anotações e os recebimentos são da equipe Synergy.",
        )
    }
}
