package br.com.synergyone.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.synergyone.android.data.auth.AuthRepository
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.ui.client.ClientHomeScreen
import br.com.synergyone.android.ui.login.LoginRoute
import br.com.synergyone.android.ui.login.LoginViewModel
import br.com.synergyone.android.ui.team.TeamHomeScreen

/**
 * Grafo de navegação do app: login é o destino inicial; a área de equipe ou cliente só é
 * alcançada após autenticação real bem-sucedida (hoje sempre pendente — ver [AuthRepository]).
 */
@Composable
fun SynergyNavGraph(
    authRepository: AuthRepository,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = NavRoutes.LOGIN) {
        composable(NavRoutes.LOGIN) {
            val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory(authRepository))
            LoginRoute(
                viewModel = viewModel,
                onNavigateToHome = { role -> navigateToHome(navController, role) },
            )
        }
        composable(NavRoutes.TEAM_HOME) {
            TeamHomeScreen()
        }
        composable(NavRoutes.CLIENT_HOME) {
            ClientHomeScreen()
        }
    }
}

private fun navigateToHome(navController: NavHostController, role: UserRole) {
    val destination = when (role) {
        UserRole.TEAM -> NavRoutes.TEAM_HOME
        UserRole.CLIENT -> NavRoutes.CLIENT_HOME
    }
    navController.navigate(destination) {
        popUpTo(NavRoutes.LOGIN) { inclusive = true }
    }
}
