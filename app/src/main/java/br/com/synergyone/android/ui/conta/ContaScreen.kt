package br.com.synergyone.android.ui.conta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.BuildConfig
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.data.settings.ThemeMode
import br.com.synergyone.android.data.settings.ThemeSettings
import br.com.synergyone.android.ui.common.initialsOf
import br.com.synergyone.android.ui.common.papelLabel
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.BadgeTone
import br.com.synergyone.android.ui.components.ScreenHeader
import br.com.synergyone.android.ui.components.StatusBadge
import br.com.synergyone.android.ui.components.SynergyCard

@Composable
fun ContaScreen(user: AuthenticatedUser, onLogout: () -> Unit, themeSettings: ThemeSettings, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val mode by themeSettings.mode.collectAsState()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (onBack != null) AppTopBar(title = "Perfil e configurações", userName = user.displayName, onAccount = null, onBack = onBack) else ScreenHeader("Conta")

        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initialsOf(user.displayName), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusBadge(
                        text = papelLabel(user.papel.wire),
                        tone = if (user.role == UserRole.TEAM) BadgeTone.Brand else BadgeTone.Info,
                    )
                }
            }
        }

        if (user.role == UserRole.CLIENT) {
            SynergyCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suas empresas", style = MaterialTheme.typography.titleMedium)
                    if (user.empresas.isEmpty()) {
                        Text(
                            "Nenhuma empresa vinculada à sua conta. Fale com a Synergy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    user.empresas.forEach { Text(it.nome, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        } else {
            SynergyCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Acesso", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (user.canWrite) "Você pode acompanhar e registrar informações das empresas."
                        else "Você pode acompanhar as empresas, sem gravar alterações.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Aparência", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(ThemeMode.Dark to "Escuro", ThemeMode.Light to "Claro").forEachIndexed { i, (m, label) ->
                        SegmentedButton(
                            selected = mode == m,
                            onClick = { themeSettings.set(m) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(label) }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Sair da conta")
        }

        Text(
            "Synergy One ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
