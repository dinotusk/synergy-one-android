package br.com.synergyone.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.di.AppContainer
import br.com.synergyone.android.ui.client.ClientHomeScreen
import br.com.synergyone.android.ui.components.BrandMark
import br.com.synergyone.android.ui.conta.ContaScreen
import br.com.synergyone.android.ui.empresas.EmpresaFichaScreen
import br.com.synergyone.android.ui.common.LocalDataFreshness
import br.com.synergyone.android.ui.leads.LeadDetailScreen
import br.com.synergyone.android.ui.leads.LeadsScreen
import br.com.synergyone.android.ui.login.LoginRoute
import br.com.synergyone.android.ui.login.LoginViewModel
import br.com.synergyone.android.ui.modules.WebLauncher
import br.com.synergyone.android.ui.team.TeamHomeScreen
import br.com.synergyone.android.ui.termometro.TermometroClienteScreen
import br.com.synergyone.android.ui.termometro.TermometroEquipeScreen
import kotlinx.coroutines.launch

/**
 * Grafo de navegação do app: a splash tenta restaurar a sessão salva; sem sessão válida vai ao
 * login. As áreas de equipe e cliente só são alcançadas com sessão autenticada e acesso confirmado;
 * as telas de detalhe (ficha da empresa, lead, perfil) vivem como rotas próprias.
 */
@Composable
fun SynergyNavGraph(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoutes.SPLASH,
) {
    val authRepository = container.authRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val user by authRepository.currentUser.collectAsState()

    val logout: () -> Unit = {
        scope.launch {
            authRepository.logout()
            navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }
    val nav = remember(navController, context) {
        object : AppNavigator {
            override fun openPerfil() = navController.navigate(NavRoutes.PERFIL)
            override fun openEmpresa(id: Long) = navController.navigate(NavRoutes.empresa(id))
            override fun openLead(id: Long) = navController.navigate(NavRoutes.lead(id))
            override fun openLeadsDaEmpresa(id: Long) = navController.navigate(NavRoutes.empresaLeads(id))
            override fun openTermometroDaEmpresa(id: Long) = navController.navigate(NavRoutes.empresaTermometro(id))
            override fun openWeb(path: String) = WebLauncher.open(context, path)
            override fun back() { navController.popBackStack() }
        }
    }

    /** Telas autenticadas: sem usuário em memória (ex.: processo recriado) volta ao login. */
    @Composable
    fun Authed(content: @Composable (AuthenticatedUser) -> Unit) {
        user?.let { content(it) } ?: BackToLogin(navController)
    }
    fun NavBackStackEntry.id(): Long = arguments?.getString(NavRoutes.ARG_ID)?.toLongOrNull() ?: 0L
    val idArg = listOf(navArgument(NavRoutes.ARG_ID) { type = NavType.StringType })

    // Sem usuário (logout, sessão expirada, troca de conta): nada da sessão anterior pode sobrar em memória.
    LaunchedEffect(user == null) { if (user == null) container.clearSessionData() }

    CompositionLocalProvider(LocalDataFreshness provides container.freshness) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavRoutes.SPLASH) {
            LaunchedEffect(Unit) {
                val restored = authRepository.restoreSession()
                if (restored != null) {
                    navigateToHome(navController, restored.role, NavRoutes.SPLASH)
                } else {
                    navController.navigate(NavRoutes.LOGIN) { popUpTo(NavRoutes.SPLASH) { inclusive = true } }
                }
            }
            // Mesmo fundo e marca do splash nativo, para a abertura não "piscar" entre telas.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BrandMark(size = 120.dp)
                CircularProgressIndicator(modifier = Modifier.padding(top = 220.dp).size(24.dp), strokeWidth = 2.5.dp)
            }
        }
        composable(NavRoutes.LOGIN) {
            val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory(authRepository))
            LoginRoute(viewModel = viewModel, onNavigateToHome = { role -> navigateToHome(navController, role, NavRoutes.LOGIN) })
        }
        composable(NavRoutes.TEAM_HOME) { Authed { TeamHomeScreen(container, it, nav, logout) } }
        composable(NavRoutes.CLIENT_HOME) { Authed { ClientHomeScreen(container, it, nav, logout) } }

        composable(NavRoutes.PERFIL) { Authed { ContaScreen(it, logout, container.themeSettings, onBack = nav::back) } }
        composable(NavRoutes.EMPRESA, idArg) { e -> Authed { EmpresaFichaScreen(e.id(), it, container, nav, logout) } }
        composable(NavRoutes.EMPRESA_LEADS, idArg) { e ->
            Authed {
                LeadsScreen(
                    repository = container.leadRepository,
                    userName = it.displayName,
                    empresas = emptyList(),
                    onAccount = nav::openPerfil,
                    onSignIn = logout,
                    fixedEmpresaId = e.id(),
                    title = "Leads",
                    onBack = nav::back,
                    onLeadClick = if (it.role == UserRole.TEAM) nav::openLead else null,
                )
            }
        }
        composable(NavRoutes.EMPRESA_TERMOMETRO, idArg) { e ->
            Authed {
                if (it.role == UserRole.TEAM) {
                    TermometroEquipeScreen(container.termometroRepository, it, nav::openPerfil, logout, fixedEmpresaId = e.id(), onBack = nav::back)
                } else {
                    TermometroClienteScreen(container.termometroRepository, container.leadRepository, it, nav::openPerfil, logout, fixedEmpresaId = e.id(), onBack = nav::back)
                }
            }
        }
        composable(NavRoutes.LEAD, idArg) { e -> Authed { LeadDetailScreen(e.id(), it, container, nav, logout) } }
    }
    }
}

/** Sem usuário em memória a área não tem o que mostrar: volta ao login. */
@Composable
private fun BackToLogin(navController: NavHostController) {
    LaunchedEffect(Unit) {
        navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
    }
}

private fun navigateToHome(navController: NavHostController, role: UserRole, from: String) {
    val destination = when (role) {
        UserRole.TEAM -> NavRoutes.TEAM_HOME
        UserRole.CLIENT -> NavRoutes.CLIENT_HOME
    }
    navController.navigate(destination) {
        popUpTo(from) { inclusive = true }
    }
}
