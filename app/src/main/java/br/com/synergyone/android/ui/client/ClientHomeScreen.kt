package br.com.synergyone.android.ui.client

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.di.AppContainer
import br.com.synergyone.android.ui.common.LoadState
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.ui.components.DashboardTab
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.home.HomeScaffold
import br.com.synergyone.android.ui.leads.LeadsScreen
import br.com.synergyone.android.ui.modules.MaisScreen
import br.com.synergyone.android.ui.modules.ModuleCatalog
import br.com.synergyone.android.ui.modules.ModuleCategory
import br.com.synergyone.android.ui.modules.ModuleHubScreen
import br.com.synergyone.android.ui.modules.NativeTab
import br.com.synergyone.android.ui.navigation.AppNavigator
import br.com.synergyone.android.ui.termometro.EmpresaOption
import br.com.synergyone.android.ui.termometro.TermometroClienteScreen

private val clientTabs = listOf(
    DashboardTab("Termômetro", Icons.Filled.Speed, "Termômetro"),
    DashboardTab("Leads", Icons.Filled.Group, "Leads"),
    DashboardTab("Entregas", Icons.Filled.Inventory2, "Entregas"),
    DashboardTab("Mais", Icons.Filled.MoreHoriz, "Mais módulos"),
)

/** Área do cliente: somente leitura, restrita às empresas da conta (a RLS garante no servidor). */
@Composable
fun ClientHomeScreen(container: AppContainer, user: AuthenticatedUser, nav: AppNavigator, onLogout: () -> Unit) {
    // Módulos habilitados em qualquer empresa do cliente decidem o que aparece em "Mais".
    val empresasLoader = rememberLoader("client-empresas") { container.empresaRepository.listar() }
    val empresasState by empresasLoader.state.collectAsState()
    val habilitados = (empresasState as? LoadState.Loaded)?.data?.flatMap { it.modulosAtivos }?.toSet() ?: emptySet()
    val modules = ModuleCatalog.visibleFor(user, habilitados)

    HomeScaffold(clientTabs) { tab, select ->
        when {
            // Logado, mas sem empresa vinculada: não há dados; só "Mais" tem o que mostrar.
            user.semEmpresa && tab != 3 -> EmptyDashboardState(
                "Sem empresa vinculada",
                "Sua conta ainda não está vinculada a uma empresa. Fale com a Synergy para liberar o acesso.",
                Modifier.padding(20.dp),
            )
            tab == 0 -> TermometroClienteScreen(container.termometroRepository, container.leadRepository, user, onAccount = nav::openPerfil, onSignIn = onLogout)
            tab == 1 -> LeadsScreen(
                container.leadRepository,
                userName = user.displayName,
                empresas = user.empresas.map { EmpresaOption(it.id, it.nome) },
                onAccount = nav::openPerfil,
                onSignIn = onLogout,
            )
            tab == 2 -> ModuleHubScreen(
                title = "Entregas",
                explanation = "O acompanhamento das entregas (playbook de 30 dias) ainda não tem API para o app. Abra no site; quando o backend publicar a rota, ele passa a funcionar aqui dentro.",
                user = user,
                modules = modules.filter { it.category == ModuleCategory.Operacao },
                nav = nav,
            )
            else -> MaisScreen(
                user = user,
                modules = modules,
                nav = nav,
                onOpenNative = { t ->
                    when (t) {
                        NativeTab.Termometro -> select(0)
                        NativeTab.Leads, NativeTab.Clientes -> select(1)
                    }
                },
                empresas = user.empresas,
            )
        }
    }
}
