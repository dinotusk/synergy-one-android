package br.com.synergyone.android.ui.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.GroupLabel
import br.com.synergyone.android.ui.components.SynergyCard
import br.com.synergyone.android.ui.navigation.AppNavigator

/**
 * "Mais": perfil e configurações + todos os módulos por categoria, com o papel da conta aplicado.
 * Cada item faz algo de verdade: abre a aba nativa ou a página do site (módulos sem API no app).
 */
@Composable
fun MaisScreen(
    user: AuthenticatedUser,
    modules: List<AppModule>,
    nav: AppNavigator,
    onOpenNative: (NativeTab) -> Unit,
    modifier: Modifier = Modifier,
    /** Empresas do cliente: cada uma abre a ficha. A equipe usa a aba Clientes. */
    empresas: List<br.com.synergyone.android.data.auth.model.EmpresaRef> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AppTopBar(title = "Mais", userName = user.displayName, onAccount = nav::openPerfil)

        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Abrir perfil e configurações", onClick = nav::openPerfil).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.synergyPrimaryText(), modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Perfil e configurações", style = MaterialTheme.typography.titleSmall)
                    Text("${user.displayName} • aparência, segurança e ajuda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (empresas.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupLabel(if (empresas.size == 1) "Minha empresa" else "Minhas empresas")
                SynergyCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        empresas.forEachIndexed { i, e ->
                            ModuleRow(e.nome, "Resultados, leads e módulos desta empresa.", external = false, onClick = { nav.openEmpresa(e.id) })
                            if (i != empresas.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        ModuleCategory.entries.forEach { categoria ->
            val itens = modules.filter { it.category == categoria }
            if (itens.isNotEmpty()) CategoryBlock(categoria, itens, nav, onOpenNative)
        }

    }
}

@Composable
private fun CategoryBlock(categoria: ModuleCategory, itens: List<AppModule>, nav: AppNavigator, onOpenNative: (NativeTab) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel(categoria.label)
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                itens.forEachIndexed { i, m ->
                    ModuleRow(
                        label = m.label,
                        description = m.description,
                        external = m.target is ModuleTarget.Web,
                        onClick = {
                            when (val t = m.target) {
                                is ModuleTarget.Native -> onOpenNative(t.tab)
                                is ModuleTarget.Web -> nav.openWeb(t.path)
                            }
                        },
                    )
                    if (i != itens.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun ModuleRow(label: String, description: String?, external: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = if (external) "Abrir no site" else "Abrir", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (external) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abre no navegador", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MaterialTheme.synergyPrimaryText() = colorScheme.primary
