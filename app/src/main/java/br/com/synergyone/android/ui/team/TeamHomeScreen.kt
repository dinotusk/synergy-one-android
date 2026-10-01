package br.com.synergyone.android.ui.team

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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
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

private val teamTabs = listOf(
    DashboardTab("Início", Icons.Filled.Home, "Início da equipe"),
    DashboardTab("Clientes", Icons.Filled.Groups, "Clientes"),
    DashboardTab("Operação", Icons.Filled.Dashboard, "Operação"),
    DashboardTab("Mais", Icons.Filled.MoreHoriz, "Mais opções"),
)

/** Dados reais devem ser injetados pelo repositório quando a API estiver disponível. */
@Composable
fun TeamHomeScreen() = TeamDashboard(showDemoContent = false)

/** Usada apenas pela atividade de demonstração debug e por previews. */
@Composable
fun TeamDemoScreen() = TeamDashboard(showDemoContent = true)

@Composable
private fun TeamDashboard(showDemoContent: Boolean) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { DashboardBottomBar(teamTabs, selectedTab) { selectedTab = it } },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            when (selectedTab) {
                0 -> TeamOverview(showDemoContent)
                1 -> SectionWithoutData("Clientes", "Os clientes autorizados aparecerão aqui quando a integração estiver disponível.")
                2 -> SectionWithoutData("Operação", "Os dados operacionais ainda não foram carregados.")
                else -> SectionWithoutData("Mais", "Configurações e recursos adicionais serão exibidos aqui.")
            }
        }
    }
}

@Composable
private fun TeamOverview(showDemoContent: Boolean) {
    DashboardHeader("Visão da equipe", if (showDemoContent) "DEMONSTRAÇÃO • dados fictícios" else "Acompanhamento da operação")
    if (!showDemoContent) {
        EmptyDashboardState("Sem dados para exibir", "O resumo aparecerá aqui assim que a API disponibilizar as informações da equipe.")
        return
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard("Entregas", "18", "no período", Modifier.weight(1f))
        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
        MetricCard("Leads", "24", "no período", Modifier.weight(1f))
    }
    ProgressCard("Progresso semanal", 0.72f, "Semana atual", Modifier.padding(top = 16.dp))
    TrendCard("Entregas concluídas", "Últimas 6 semanas", "entregas", listOf(4f, 7f, 5f, 10f, 12f, 18f), Modifier.padding(top = 16.dp))
    EmptyDashboardState("Próximos compromissos", "Este bloco receberá eventos reais do calendário da equipe.", Modifier.padding(top = 16.dp))
}

@Composable
private fun SectionWithoutData(title: String, detail: String) {
    DashboardHeader(title, "Dados atualizados pela sua conta")
    EmptyDashboardState("Nada para mostrar ainda", detail)
}

@Preview(showBackground = true, backgroundColor = 0xFF121016)
@Composable
private fun TeamDashboardDemoPreview() {
    SynergyOneTheme { TeamDashboard(showDemoContent = true) }
}
