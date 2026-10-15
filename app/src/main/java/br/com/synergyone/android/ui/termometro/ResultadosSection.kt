package br.com.synergyone.android.ui.termometro

import br.com.synergyone.android.ui.theme.synergy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.api.FunilDto
import br.com.synergyone.android.data.api.MetaDto
import br.com.synergyone.android.data.api.ResultadosDto
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.common.etapaLabel
import br.com.synergyone.android.ui.common.formatMoney
import br.com.synergyone.android.ui.common.formatNumber
import br.com.synergyone.android.ui.common.formatPercent
import br.com.synergyone.android.ui.components.ChartPoint
import br.com.synergyone.android.ui.components.EvolutionCard
import br.com.synergyone.android.ui.components.SectionTitle
import br.com.synergyone.android.ui.components.SynergyCard

/**
 * Resultados de um período (ao vivo na equipe, congelados no cliente), em ordem de importância:
 * principais → financeiro → secundários agrupados → funil → evolução. Cada bloco só aparece quando
 * há dado: sem histórico não se fabrica gráfico, e cliques ausentes viram aviso, não zero.
 */
@Composable
fun ResultadosSection(
    resultados: ResultadosDto,
    periodo: String,
    modifier: Modifier = Modifier,
    titulo: String = "Resultados",
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(titulo, eyebrow = periodo)

        // Período sem nenhum lead nem venda: uma mensagem só, em vez de uma grade de zeros.
        if (resultados.totalLeads == 0 && resultados.ganhos == 0 && resultados.perdidos == 0 && resultados.vendidoCentavos == 0L) {
            InfoNote("Sem movimento neste período", "Nenhum lead foi captado nem venda registrada entre estas datas. Tente outro período.")
            resultados.cliques?.takeIf { !it.disponivel }?.let { InfoNote("Cliques ainda indisponíveis", it.pendencia ?: "Esta métrica ainda não é coletada.") }
            return@Column
        }

        HighlightsCard(resultados)
        FinanceCard(resultados)
        SecondaryStatsCard(resultados)

        if (resultados.funil.isNotEmpty()) FunilCard(resultados.funil)

        // Sem pelo menos dois pontos não há evolução: o cartão mostra o aviso honesto.
        EvolutionCard(
            title = "Leads captados",
            unit = "leads por ${if (resultados.evolucao.size >= 2) "intervalo" else "período"}, pela data de captura",
            period = periodo,
            points = resultados.evolucao.map { ChartPoint(it.rotulo, it.captados.toFloat()) },
        )

        // Vendas e valor vendido usam a mesma série `evolucao` já entregue pela API; só aparecem com valor real.
        if (resultados.evolucao.size >= 2 && resultados.evolucao.any { it.ganhos > 0 }) {
            EvolutionCard(
                title = "Vendas fechadas",
                unit = "vendas por intervalo, pela data de fechamento",
                period = periodo,
                points = resultados.evolucao.map { ChartPoint(it.rotulo, it.ganhos.toFloat()) },
            )
        }
        if (resultados.evolucao.size >= 2 && resultados.evolucao.any { it.vendidoCentavos > 0 }) {
            EvolutionCard(
                title = "Valor vendido",
                unit = "reais por intervalo, pela data de fechamento",
                period = periodo,
                points = resultados.evolucao.map { ChartPoint(it.rotulo, it.vendidoCentavos / 100f) },
                format = { formatMoney((it * 100).toLong()) },
            )
        }

        resultados.cliques?.takeIf { !it.disponivel }?.let {
            InfoNote("Cliques ainda indisponíveis", it.pendencia ?: "Esta métrica ainda não é coletada.")
        }
    }
}

/** Fontes ampliadas (acessibilidade): empilha em vez de espremer três colunas. */
@Composable
internal fun isLargeFont() = LocalConfiguration.current.fontScale > 1.3f

@Composable
internal fun Stat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = "$label: $value" }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HighlightsCard(r: ResultadosDto) {
    SynergyCard(modifier = Modifier.fillMaxWidth()) {
        val stats: @Composable (Modifier) -> Unit = { m ->
            Stat("Leads", r.totalLeads.toString(), m)
            Stat("Vendas", r.ganhos.toString(), m)
            Stat("Conversão", formatPercent(r.conversao), m)
        }
        if (isLargeFont()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { stats(Modifier) }
        } else {
            Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { stats(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun FinanceCard(r: ResultadosDto) {
    SynergyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Valores em reais são largos: empilha já a partir de 1,15×.
            if (LocalConfiguration.current.fontScale > 1.15f) {
                Stat("Vendido", formatMoney(r.vendidoCentavos))
                Stat("Recebido", formatMoney(r.recebidoCentavos), valueColor = MaterialTheme.colorScheme.primary)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("Vendido", formatMoney(r.vendidoCentavos), Modifier.weight(1f))
                    Stat("Recebido", formatMoney(r.recebidoCentavos), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                }
            }
            if (r.vendidoCentavos > 0) {
                val fracao = (r.recebidoCentavos.toFloat() / r.vendidoCentavos).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fracao },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Text(
                    "${(fracao * 100).toInt()}% do vendido já foi recebido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("Sem vendas no período.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun OrigemCard(origens: List<Pair<String, Int>>, total: Int, semOrigem: Int) {
    val maximo = origens.maxOf { it.second }.coerceAtLeast(1)
    SynergyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Origem dos leads", style = MaterialTheme.typography.titleMedium)
                Text("origem registrada em cada lead • $total no período", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            origens.take(6).forEach { (nome, qtd) ->
                Column(modifier = Modifier.clearAndSetSemantics { contentDescription = "$nome: $qtd" }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nome, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(qtd.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(qtd.toFloat() / maximo).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
            if (semOrigem > 0) {
                Text("Origem não informada: $semOrigem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Indicadores de apoio juntos em um só cartão (linhas finas), em vez de uma pilha de caixas iguais. */
@Composable
private fun SecondaryStatsCard(r: ResultadosDto) {
    val linhas = buildList {
        add("Tempo até a venda" to (r.diasAteVenda?.media?.let { "${formatNumber(it)} dias" } ?: "—"))
        r.diasAteVenda?.mediana?.let { add("Mediana até a venda" to "${formatNumber(it)} dias") }
        add("Vendas perdidas" to r.perdidos.toString())
        r.emAndamento?.let { add("Em andamento" to it.toString()) }
    }
    SynergyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
            linhas.forEachIndexed { i, (rotulo, valor) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clearAndSetSemantics { contentDescription = "$rotulo: $valor" }, verticalAlignment = Alignment.CenterVertically) {
                    Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(valor, style = MaterialTheme.typography.labelLarge)
                }
                if (i != linhas.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
internal fun FunilCard(funil: List<FunilDto>) {
    val maximo = funil.maxOf { it.total }.coerceAtLeast(1)
    val base = funil.firstOrNull()?.total?.takeIf { it > 0 }
    SynergyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Funil de vendas", style = MaterialTheme.typography.titleMedium)
                Text("leads por etapa • % sobre o total captado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            funil.forEach { etapa ->
                val rotuloEtapa = etapa.rotulo ?: etapaLabel(etapa.etapa)
                val pct = base?.let { ", ${(etapa.total * 100f / it).toInt()}% do total" }.orEmpty()
                Column(modifier = Modifier.clearAndSetSemantics { contentDescription = "$rotuloEtapa: ${etapa.total}$pct" }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(etapa.rotulo ?: etapaLabel(etapa.etapa), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(etapa.total.toString(), style = MaterialTheme.typography.labelLarge)
                        if (base != null) {
                            Text(
                                " • ${(etapa.total * 100f / base).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(etapa.total.toFloat() / maximo)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetasSection(titulo: String, metas: List<MetaDto>, modifier: Modifier = Modifier) {
    if (metas.isEmpty()) return
    SynergyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            metas.forEach { meta ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(meta.rotulo.ifBlank { meta.indicador }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (meta.alvo != null && meta.realizado != null) {
                            Text(
                                "${formatNumber(meta.realizado)} de ${formatNumber(meta.alvo)}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    if (meta.alvo != null && meta.alvo > 0 && meta.realizado != null) {
                        LinearProgressIndicator(
                            progress = { (meta.realizado / meta.alvo).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                    }
                    meta.pendencia?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
