package br.com.synergyone.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.theme.HeroFrom
import br.com.synergyone.android.ui.theme.HeroGlowCobalt
import br.com.synergyone.android.ui.theme.HeroGlowViolet
import br.com.synergyone.android.ui.theme.HeroTo
import br.com.synergyone.android.ui.theme.HeroVia
import br.com.synergyone.android.ui.theme.synergy

data class DashboardTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
)

/** Cartão padrão do site: fundo de superfície, borda fina e raio generoso. */
@Composable
fun SynergyCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

/** Rótulo discreto de grupo (Configurações, Mais): evita repetir "Módulos" em cada bloco. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp).semantics { heading() },
    )
}

/** Cabeçalho de seção do site: sobretítulo em violeta + título em Sora. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, eyebrow: String? = null) {
    Column(modifier = modifier) {
        if (eyebrow != null) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.synergy.primaryText,
            )
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

enum class BadgeTone { Success, Warning, Danger, Info, Neutral, Brand }

/** Selo em pílula, como o StatusBadge do site (fundo 15% + borda 30% da cor do estado). */
@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        BadgeTone.Success -> MaterialTheme.synergy.success
        BadgeTone.Warning -> MaterialTheme.synergy.warning
        BadgeTone.Danger -> MaterialTheme.synergy.danger
        BadgeTone.Info -> MaterialTheme.synergy.info
        BadgeTone.Brand -> MaterialTheme.synergy.primaryText
        BadgeTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun DashboardBottomBar(
    tabs: List<DashboardTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        // Com fonte muito ampliada os rótulos não cabem: a barra mostra só os ícones (cada um com
        // descrição para leitores de tela), em vez de quebrar ou cortar palavras.
        val largeFont = LocalConfiguration.current.fontScale > 1.3f
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    icon = { Icon(tab.icon, contentDescription = tab.contentDescription) },
                    label = if (largeFont) null else ({ Text(tab.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false) }),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.synergy.primaryText,
                        selectedTextColor = MaterialTheme.synergy.primaryText,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
fun EmptyDashboardState(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Chip de filtro: o selecionado usa o lilás do tema (visível no claro e no escuro), não só um cinza sutil. */
@Composable
fun synergyChipColors() = androidx.compose.material3.FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.synergy.primaryText,
    selectedLeadingIconColor = MaterialTheme.synergy.primaryText,
)
