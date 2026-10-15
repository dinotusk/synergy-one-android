package br.com.synergyone.android.ui.termometro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.common.initialsOf
import br.com.synergyone.android.ui.components.BrandMark

/** Botão-seletor em pílula: ícone + valor atual + seta. Abre um menu/folha ao tocar. */
@Composable
fun SelectorChip(
    icon: ImageVector,
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .semantics { contentDescription = "$description: $label" },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (enabled) Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

data class EmpresaOption(val id: Long, val nome: String, val classificacao: Classificacao = Classificacao.NaoAvaliado)

@Composable
fun EmpresaSelector(
    empresas: List<EmpresaOption>,
    selecionadaId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    /** Quando informado, a lista ganha a opção "todas" (selecionada = `null`) e o seletor aceita `null`. */
    todasLabel: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    val atual = empresas.firstOrNull { it.id == selecionadaId } ?: if (todasLabel == null) empresas.firstOrNull() else null
    Box(modifier) {
        SelectorChip(
            icon = Icons.Filled.Business,
            label = atual?.nome ?: todasLabel ?: "Sem empresa",
            description = "Empresa",
            enabled = empresas.size > 1,
            modifier = Modifier.fillMaxWidth(),
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (todasLabel != null) {
                DropdownMenuItem(text = { Text(todasLabel) }, onClick = { open = false; onSelect(null) })
            }
            empresas.forEach { e ->
                DropdownMenuItem(
                    text = { Text(e.nome) },
                    leadingIcon = { ClassificationDot(e.classificacao) },
                    onClick = { open = false; onSelect(e.id) },
                )
            }
        }
    }
}

@Composable
fun PeriodSelectorChip(label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SelectorChip(Icons.Filled.CalendarMonth, label, "Período", modifier.fillMaxWidth(), enabled, onClick)
}
