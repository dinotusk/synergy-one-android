package br.com.synergyone.android.ui.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.components.DashboardBottomBar
import br.com.synergyone.android.ui.components.DashboardHeader
import br.com.synergyone.android.ui.components.DashboardTab
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.MetricCard
import br.com.synergyone.android.ui.components.ProgressCard
import br.com.synergyone.android.ui.components.TrendCard
import br.com.synergyone.android.ui.theme.SynergyOneTheme

private val clientTabs = listOf(
    DashboardTab("Início", Icons.Filled.Home, "Início do cliente"),
    DashboardTab("Entregas", Icons.Filled.Inventory2, "Entregas"),
    DashboardTab("Biblioteca", Icons.AutoMirrored.Filled.MenuBook, "Biblioteca"),
    DashboardTab("Mais", Icons.Filled.MoreHoriz, "Mais opções"),
)

/** A conta autenticada pelo backend definirá quais dados chegam nesta tela. */
@Composable
fun ClientHomeScreen() = ClientDashboard(showDemoContent = false)

/** Usada apenas pela atividade de demonstração debug e por previews. */
@Composable
fun ClientDemoScreen() = ClientDashboard(showDemoContent = true)

@Composable
private fun ClientDashboard(showDemoContent: Boolean) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { DashboardBottomBar(clientTabs, selectedTab) { selectedTab = it } },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            when (selectedTab) {
                0 -> ClientOverview(showDemoContent)
                1 -> SectionWithoutData("Entregas", "Suas entregas serão carregadas quando a integração estiver disponível.")
                2 -> SectionWithoutData("Biblioteca", "Os materiais liberados para sua empresa aparecerão aqui.")
                else -> SectionWithoutData("Mais", "Configurações e recursos adicionais serão exibidos aqui.")
            }
        }
    }
}

@Composable
private fun ClientOverview(showDemoContent: Boolean) {
    DashboardHeader("Sua empresa", if (showDemoContent) "DEMONSTRAÇÃO • dados fictícios" else "Acompanhe os resultados da sua operação")
    if (!showDemoContent) {
        EmptyDashboardState("Dados ainda indisponíveis", "As entregas e a evolução das mídias aparecerão aqui após a sincronização com a sua conta.")
        return
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard("Entregas", "8", "no período", Modifier.weight(1f))
        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
        MetricCard("Biblioteca", "12", "itens liberados", Modifier.weight(1f))
    }
    ProgressCard("Plano de entregas", 0.56f, "Outubro", Modifier.padding(top = 16.dp))
    TrendCard("Evolução das mídias", "Últimos 6 meses", "índice", listOf(18f, 24f, 22f, 31f, 37f, 44f), Modifier.padding(top = 16.dp))
    EmptyDashboardState("Biblioteca da empresa", "Este bloco receberá os materiais autorizados para a sua organização.", Modifier.padding(top = 16.dp))
}

@Composable
private fun SectionWithoutData(title: String, detail: String) {
    DashboardHeader(title, "Conteúdo exclusivo da sua empresa")
    EmptyDashboardState("Nada para mostrar ainda", detail)
}

@Preview(showBackground = true, backgroundColor = 0xFF121016)
@Composable
private fun ClientDashboardDemoPreview() {
    SynergyOneTheme { ClientDashboard(showDemoContent = true) }
}
