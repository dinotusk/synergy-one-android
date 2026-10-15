package br.com.synergyone.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.common.formatNumber

data class ChartPoint(val label: String, val value: Float)

/**
 * Cartão de evolução: título + unidade + período, eixo Y com o máximo, rótulos do primeiro e do
 * último ponto. Com menos de dois pontos não há evolução: mostra um aviso honesto, sem gráfico.
 */
@Composable
fun EvolutionCard(
    title: String,
    unit: String,
    period: String,
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    format: (Double) -> String = { formatNumber(it) },
) {
    SynergyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("$unit • $period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (points.size < 2) {
                Text(
                    "Ainda não há histórico suficiente para desenhar a evolução. Ela aparece quando houver pelo menos dois pontos no período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LineChart(points, unit, format)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(points.first().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(points.last().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LineChart(points: List<ChartPoint>, unit: String, format: (Double) -> String) {
    val line = MaterialTheme.colorScheme.primary
    val glow = MaterialTheme.synergyGlow()
    val grid = MaterialTheme.colorScheme.outlineVariant
    val max = points.maxOf { it.value }.coerceAtLeast(1f)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.height(120.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(format(max.toDouble()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(120.dp)
                .semantics { contentDescription = "Gráfico de $unit, de ${points.first().label} a ${points.last().label}, máximo ${format(max.toDouble())}" },
        ) {
            val path = Path()
            val pad = 4.dp.toPx()
            val h = size.height - pad * 2
            points.forEachIndexed { i, p ->
                val x = size.width * i / points.lastIndex
                val y = pad + h - (p.value / max * h)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            listOf(0f, 0.5f, 1f).forEach { f ->
                val y = pad + h * f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val area = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(area, Brush.verticalGradient(listOf(glow.copy(alpha = 0.22f), Color.Transparent)))
            drawPath(path, line, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val last = points.last()
            drawCircle(line, 4.dp.toPx(), Offset(size.width, pad + h - (last.value / max * h)))
        }
    }
}

@Composable
private fun MaterialTheme.synergyGlow(): Color = colorScheme.primary
