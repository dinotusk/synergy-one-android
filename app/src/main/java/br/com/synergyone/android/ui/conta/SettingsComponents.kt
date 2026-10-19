package br.com.synergyone.android.ui.conta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.components.GroupLabel
import br.com.synergyone.android.ui.components.SynergyCard

/** Grupo de configurações: rótulo discreto + um cartão com as linhas separadas por divisores finos. */
@Composable
fun SettingsGroup(label: String, modifier: Modifier = Modifier, footer: String? = null, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel(label)
        SynergyCard(modifier = Modifier.fillMaxWidth()) { Column { content() } }
        if (footer != null) {
            Text(footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
fun SettingsDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

/**
 * Linha de configuração. Só é clicável (com feedback de toque) quando há [onClick]; linhas
 * informativas não têm ripple nem seta, para não sugerir uma ação que não existe.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    external: Boolean = false,
    checked: Boolean = false,
    /** Opção de uma escolha única (aparência): o leitor anuncia "selecionado" em vez de um ícone solto. */
    selectable: Boolean = false,
    destructive: Boolean = false,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    // Fonte muito ampliada: valores longos descem para baixo do título em vez de quebrar no meio da palavra.
    val empilhar = value != null && value.length > 14 && LocalConfiguration.current.fontScale > 1.3f
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .let {
                when {
                    onClick == null -> it
                    selectable -> it.selectable(selected = checked, role = Role.RadioButton, onClick = onClick)
                    else -> it.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
                }
            }
            .semantics(mergeDescendants = true) {} // "Papel, Gestão" como um item só para o leitor de tela
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (destructive) color else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(modifier = if (value == null || empilhar) Modifier.weight(1f) else Modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
            if (empilhar) Text(value!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (value != null && !empilhar) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        if (external) Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abre no navegador", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
