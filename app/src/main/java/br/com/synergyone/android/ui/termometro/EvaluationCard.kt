package br.com.synergyone.android.ui.termometro

import br.com.synergyone.android.ui.theme.synergy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.components.SynergyCard

private val escala = listOf(Classificacao.Vermelho, Classificacao.Amarelo, Classificacao.Verde, Classificacao.Azul, Classificacao.Lilas)

/**
 * Escala de cinco cores do Termômetro. A atual fica em destaque; as demais, esmaecidas. O texto
 * da classificação sempre aparece junto (a cor sozinha não basta). Sem avaliação, nada acende.
 */
@Composable
fun ClassificationScale(atual: Classificacao, modifier: Modifier = Modifier) {
    val posicao = escala.indexOf(atual)
    Column(
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = if (posicao >= 0) "Escala de cinco níveis: ${atual.label}, nível ${posicao + 1} de 5" else "Escala de cinco níveis: sem avaliação"
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            escala.forEachIndexed { i, c ->
                val ativo = i == posicao
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (ativo) 10.dp else 6.dp)
                        .align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(c.color().copy(alpha = if (ativo) 1f else if (posicao < 0) 0.18f else 0.30f)),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Perigo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Excelente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Card principal: classificação, escala, justificativa e próximos passos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvaluationCard(
    eyebrow: String,
    classificacao: Classificacao,
    periodo: String?,
    justificativa: String?,
    feedback: String?,
    rodape: String?,
    modifier: Modifier = Modifier,
) {
    // Sem avaliação o rótulo "publicada"/"anterior" seria falso: usa o neutro.
    val semAvaliacao = classificacao == Classificacao.NaoAvaliado && justificativa.isNullOrBlank() && feedback.isNullOrBlank()
    SynergyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text((if (semAvaliacao) "Avaliação" else eyebrow).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.synergy.primaryText)
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    ClassificationBadge(classificacao)
                    if (periodo != null) Text(periodo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = false)
                }
            }
            ClassificationScale(classificacao)

            if (semAvaliacao) {
                Text(
                    "Assim que a equipe da Synergy publicar a avaliação do período, ela aparece aqui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!justificativa.isNullOrBlank()) {
                Text(justificativa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (!feedback.isNullOrBlank()) FeedbackBlock(feedback)
            if (rodape != null) Text(rodape, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Próximos passos" com a barra violeta lateral (como as citações do site). */
@Composable
private fun FeedbackBlock(texto: String) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
        Column(modifier = Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Próximos passos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
