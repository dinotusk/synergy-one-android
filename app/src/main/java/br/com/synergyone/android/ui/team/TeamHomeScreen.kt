package br.com.synergyone.android.ui.team

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.di.AppContainer
import br.com.synergyone.android.ui.common.LoadState
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.ui.components.DashboardTab
import br.com.synergyone.android.ui.home.HomeScaffold
import br.com.synergyone.android.ui.modules.ModuleCatalog
import br.com.synergyone.android.ui.modules.ModuleCategory
import br.com.synergyone.android.ui.modules.MaisScreen
import br.com.synergyone.android.ui.modules.ModuleHubScreen
import br.com.synergyone.android.ui.modules.NativeTab
import br.com.synergyone.android.ui.navigation.AppNavigator
import br.com.synergyone.android.ui.termometro.EmpresaOption
import br.com.synergyone.android.ui.termometro.TermometroEquipeScreen

private val teamTabs = listOf(
    DashboardTab("Termômetro", Icons.Filled.Speed, "Termômetro"),
    DashboardTab("Clientes", Icons.Filled.Business, "Clientes"),
    DashboardTab("Operação", Icons.Filled.Dashboard, "Operação"),
    DashboardTab("Mais", Icons.Filled.MoreHoriz, "Mais módulos"),
)

/** Área da equipe: o papel (`leitor`, `operacao`, `gestao`, `admin`) define o que a API permite gravar. */
@Composable
fun TeamHomeScreen(container: AppContainer, user: AuthenticatedUser, nav: AppNavigator, onLogout: () -> Unit) {
    // A equipe filtra leads por empresa: a lista vem da API (a RLS decide quais ela vê).
    val empresasLoader = rememberLoader("team-empresas") { container.empresaRepository.listar() }
    val empresasState by empresasLoader.state.collectAsState()
    val empresas = (empresasState as? LoadState.Loaded)?.data?.map { EmpresaOption(it.id, it.nome) }.orEmpty()
    var segment by rememberSaveable { mutableIntStateOf(0) }

    val modules = ModuleCatalog.visibleFor(user)

    HomeScaffold(teamTabs) { tab, select ->
        Box(Modifier) {
            when (tab) {
                0 -> TermometroEquipeScreen(container.termometroRepository, user, onAccount = nav::openPerfil, onSignIn = onLogout)
                1 -> ClientesScreen(container, user, empresas, segment, { segment = it }, nav, onLogout)
                2 -> ModuleHubScreen(
                    title = "Operação",
                    explanation = "Projetos, entregas, aprovações e agenda ainda não têm API para o app. Abra o módulo no site; assim que o backend publicar as rotas, eles passam a funcionar aqui dentro.",
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
                            NativeTab.Clientes -> { segment = 0; select(1) }
                            NativeTab.Leads -> { segment = 1; select(1) }
                            NativeTab.Biblioteca -> nav.openBiblioteca()
                        }
                    },
                )
            }
        }
    }
}
