package br.com.synergyone.android.ui.conta

import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.BuildConfig
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.data.settings.ThemeMode
import br.com.synergyone.android.data.settings.ThemeSettings
import br.com.synergyone.android.ui.common.initialsOf
import br.com.synergyone.android.ui.common.papelLabel
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.ScreenHeader
import br.com.synergyone.android.ui.modules.ModuleCatalog
import br.com.synergyone.android.ui.modules.WebLauncher

/**
 * Configurações em grupos: Conta, Aparência, Segurança e acesso, Ajuda e privacidade, Sobre.
 * Só há controles que funcionam hoje; notificações, biometria e gerenciamento de sessões não aparecem
 * porque ainda não existem no app nem na API.
 */
@Composable
fun ContaScreen(user: AuthenticatedUser, onLogout: () -> Unit, themeSettings: ThemeSettings, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val mode by themeSettings.mode.collectAsState()
    val context = LocalContext.current
    var confirmarSaida by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (onBack != null) AppTopBar(title = "Configurações", userName = user.displayName, onAccount = null, onBack = onBack) else ScreenHeader("Configurações")

        SettingsGroup("Conta") {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text(initialsOf(user.displayName), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SettingsDivider()
            SettingsRow("Papel", value = papelLabel(user.papel.wire))
            SettingsDivider()
            if (user.role == UserRole.CLIENT) {
                SettingsRow(
                    if (user.empresas.size == 1) "Empresa" else "Empresas",
                    value = user.empresas.joinToString(", ") { it.nome }.ifBlank { "Nenhuma vinculada" },
                )
            } else {
                SettingsRow("Empresas", value = if (user.todasEmpresas) "Todas" else user.empresas.joinToString(", ") { it.nome }.ifBlank { "—" })
                SettingsDivider()
                SettingsRow("Permissão", value = if (user.canWrite) "Acompanha e registra" else "Somente leitura")
            }
        }

        SettingsGroup("Aparência", footer = "O padrão do app é o escuro. \"Sistema\" acompanha o tema do celular.") {
            val opcoes = listOf(ThemeMode.Dark to "Escuro", ThemeMode.Light to "Claro", ThemeMode.System to "Sistema")
            opcoes.forEachIndexed { i, (m, label) ->
                if (i > 0) SettingsDivider()
                SettingsRow(label, checked = mode == m, selectable = true, onClickLabel = "Usar tema $label", onClick = { themeSettings.set(m) })
            }
        }

        SettingsGroup("Segurança e acesso") {
            SettingsRow(
                "Recuperar ou alterar senha",
                subtitle = "Abre o site, em \"Esqueci minha senha\". O link vai só para contas ativas.",
                icon = Icons.Filled.Lock,
                external = true,
                onClickLabel = "Abrir a página de entrada no navegador",
                onClick = { WebLauncher.open(context, "/entrar") },
            )
            SettingsDivider()
            SettingsRow(
                "Sair deste aparelho",
                icon = Icons.AutoMirrored.Filled.Logout,
                destructive = true,
                onClick = { confirmarSaida = true },
            )
        }

        SettingsGroup("Ajuda e privacidade") {
            val icones = listOf(Icons.Filled.Description, Icons.Filled.Policy)
            ModuleCatalog.legal.forEachIndexed { i, (label, path) ->
                if (i > 0) SettingsDivider()
                SettingsRow(label, icon = icones.getOrNull(i), external = true, onClickLabel = "Abrir no navegador", onClick = { WebLauncher.open(context, path) })
            }
        }

        SettingsGroup("Sobre") {
            SettingsRow("Aplicativo", value = "Synergy One")
            SettingsDivider()
            SettingsRow("Versão", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            SettingsDivider()
            SettingsRow("Serviço", value = BuildConfig.API_BASE_URL.toUri().host.orEmpty())
        }
    }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair deste aparelho?") },
            text = { Text("Você precisará entrar de novo neste celular. Seus outros aparelhos não são afetados.") },
            confirmButton = { TextButton(onClick = { confirmarSaida = false; onLogout() }) { Text("Sair", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Cancelar") } },
        )
    }
}
