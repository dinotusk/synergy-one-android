package br.com.synergyone.android.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.SynergyCard
import br.com.synergyone.android.ui.navigation.AppNavigator

/**
 * Aba de um grupo cujos módulos ainda **não têm API para o app** (hoje: Operação da equipe e
 * Entregas do cliente). Em vez de uma tela vazia, diz com clareza o que acontece e abre o módulo no
 * site. Quando o backend publicar as rotas, cada item vira tela nativa.
 */
@Composable
fun ModuleHubScreen(
    title: String,
    explanation: String,
    user: AuthenticatedUser,
    modules: List<AppModule>,
    nav: AppNavigator,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTopBar(title = title, userName = user.displayName, onAccount = nav::openPerfil)
        InfoNote("Disponível no site", explanation)
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                modules.forEachIndexed { i, m ->
                    ModuleRow(m.label, m.description, external = true, onClick = { (m.target as? ModuleTarget.Web)?.let { nav.openWeb(it.path) } })
                    if (i != modules.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
