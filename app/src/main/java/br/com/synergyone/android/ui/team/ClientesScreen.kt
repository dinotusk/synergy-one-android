package br.com.synergyone.android.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.di.AppContainer
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.empresas.EmpresasSection
import br.com.synergyone.android.ui.leads.LeadsScreen
import br.com.synergyone.android.ui.navigation.AppNavigator
import br.com.synergyone.android.ui.termometro.EmpresaOption

/** Aba Clientes da equipe: as empresas (com ficha) e os leads e vendas, lado a lado. */
@Composable
fun ClientesScreen(
    container: AppContainer,
    user: AuthenticatedUser,
    empresas: List<EmpresaOption>,
    segment: Int,
    onSegment: (Int) -> Unit,
    nav: AppNavigator,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTopBar(title = "Clientes", userName = user.displayName, onAccount = nav::openPerfil)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Empresas", "Leads e vendas").forEachIndexed { i, label ->
                    SegmentedButton(selected = segment == i, onClick = { onSegment(i) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                }
            }
        }
        if (segment == 0) {
            EmpresasSection(
                repository = container.empresaRepository,
                emptyTitle = "Nenhuma empresa",
                emptyDetail = "Sua conta ainda não tem empresas visíveis.",
                modifier = Modifier.weight(1f),
                onSignIn = onSignIn,
                onEmpresaClick = nav::openEmpresa,
            )
        } else {
            LeadsScreen(
                repository = container.leadRepository,
                userName = user.displayName,
                empresas = empresas,
                onAccount = nav::openPerfil,
                onSignIn = onSignIn,
                modifier = Modifier.weight(1f),
                withHeader = false,
                onLeadClick = nav::openLead,
            )
        }
    }
}
