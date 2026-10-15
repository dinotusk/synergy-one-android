package br.com.synergyone.android.ui.termometro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.theme.synergy

/**
 * Classificação do Termômetro. Sempre comunicada por **cor + ícone + texto** (nunca só cor).
 * Ausência de avaliação é estado neutro ([NaoAvaliado]), jamais vermelho por padrão.
 */
enum class Classificacao(val wire: String?, val label: String, val icon: ImageVector) {
    Vermelho("vermelho", "Perigo", Icons.Filled.Error),
    Amarelo("amarelo", "Atenção", Icons.Filled.Warning),
    Verde("verde", "Meta alcançada", Icons.Filled.CheckCircle),
    Azul("azul", "Meta ultrapassada", Icons.AutoMirrored.Filled.TrendingUp),
    Lilas("lilas", "Excelente", Icons.Filled.AutoAwesome),
    NaoAvaliado(null, "Ainda não avaliado", Icons.Filled.HourglassEmpty);

    companion object {
        fun fromWire(value: String?): Classificacao = entries.firstOrNull { it.wire != null && it.wire == value } ?: NaoAvaliado
    }
}

@Composable
fun Classificacao.color(): Color = when (this) {
    Classificacao.Vermelho -> MaterialTheme.synergy.danger
    Classificacao.Amarelo -> MaterialTheme.synergy.warning
    Classificacao.Verde -> MaterialTheme.synergy.success
    Classificacao.Azul -> MaterialTheme.synergy.info
    Classificacao.Lilas -> MaterialTheme.synergy.lilac
    Classificacao.NaoAvaliado -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun ClassificationBadge(classificacao: Classificacao, modifier: Modifier = Modifier) {
    val color = classificacao.color()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(classificacao.icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(classificacao.label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Ponto de cor compacto para listas (o texto da classificação vem ao lado). */
@Composable
fun ClassificationDot(classificacao: Classificacao, modifier: Modifier = Modifier) {
    Icon(
        classificacao.icon,
        contentDescription = classificacao.label,
        tint = classificacao.color(),
        modifier = modifier.size(20.dp),
    )
}
