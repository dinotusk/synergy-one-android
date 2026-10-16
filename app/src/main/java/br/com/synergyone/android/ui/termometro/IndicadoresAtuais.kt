package br.com.synergyone.android.ui.termometro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.api.IndicadoresDto
import br.com.synergyone.android.data.api.SiteResumoDto
import br.com.synergyone.android.data.api.VariacaoDto
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.common.LoadStateContent
import br.com.synergyone.android.ui.common.LoaderViewModel
import br.com.synergyone.android.ui.common.formatMoney
import br.com.synergyone.android.ui.common.formatNumber
import br.com.synergyone.android.ui.common.formatPercent
import br.com.synergyone.android.ui.common.formatPeriod
import br.com.synergyone.android.ui.components.ChartPoint
import br.com.synergyone.android.ui.components.EvolutionCard
import br.com.synergyone.android.ui.components.SectionTitle
import br.com.synergyone.android.ui.components.SynergyCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Indicadores **atuais** da empresa, calculados **no servidor** (`GET /indicadores`) pelo mesmo serviço da web.
 * O app só exibe: não soma lista de leads nem faz conta de período. Não são os números congelados de uma avaliação.
 */
@Composable
fun IndicadoresAtuais(
    loader: LoaderViewModel<IndicadoresDto>,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by loader.state.collectAsState()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LoadStateContent(estado, onRetry = loader::reload, onSignIn = onSignIn) { d -> IndicadoresConteudo(d) }
    }
}

private val horaBrasilia = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", Locale("pt", "BR")).withZone(ZoneId.of("America/Sao_Paulo"))

internal fun calculadoEmTexto(iso: String?): String? =
    iso?.let { runCatching { horaBrasilia.format(Instant.parse(it)) }.getOrNull() }

/** "+25% em relação ao período anterior"; sem base (`null`) nunca vira 0%. */
internal fun variacaoTexto(v: VariacaoDto?): String {
    if (v == null) return "Sem período anterior para comparar"
    val sinal = if (v.valor > 0) "+" else if (v.valor < 0) "−" else ""
    val numero = formatNumber(kotlin.math.abs(v.valor))
    return if (v.tipo == "pontos") "$sinal$numero pontos em relação ao período anterior" else "$sinal$numero% em relação ao período anterior"
}

@Composable
private fun IndicadoresConteudo(d: IndicadoresDto) {
    val a = d.atual
    val periodo = formatPeriod(d.periodo?.de ?: a.periodo?.de, d.periodo?.ate ?: a.periodo?.ate)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Indicadores atuais do período", eyebrow = periodo)
        InfoNote(
            "Calculados no servidor, não congelados",
            "Estes números vêm do Synergy One" + (calculadoEmTexto(d.calculadoEm)?.let { " (calculados em $it, horário de Brasília)" } ?: "") +
                " e mudam conforme os leads avançam. Não são os números congelados de uma avaliação publicada.",
        )

        if (a.captados == 0 && a.vendasNoPeriodo == 0 && a.vendidoCentavos == 0L && a.recebidoCentavos == 0L) {
            InfoNote("Sem movimento neste período", "Nenhum lead foi captado nem venda fechada entre estas datas. Tente outro período.")
        } else {
            Text("Leads captados no período", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            SynergyCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val stats: @Composable (Modifier) -> Unit = { m ->
                        Stat("Leads captados", a.captados.toString(), m)
                        Stat("Já ganhos", a.ganhos.toString(), m)
                        Stat("Conversão", formatPercent(a.conversao), m)
                    }
                    if (isLargeFont()) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { stats(Modifier) }
                    else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { stats(Modifier.weight(1f)) }
                    Text(
                        "Conversão = leads captados no período que já foram ganhos ÷ leads captados no período.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Leads: ${variacaoTexto(d.comparacao?.leads)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SynergyCard(modifier = Modifier.fillMaxWidth()) {
                val linhas = buildList {
                    add("Novos" to a.porEtapa.novo.toString())
                    add("Contatados" to a.porEtapa.contatado.toString())
                    add("Em negociação" to a.porEtapa.negociacao.toString())
                    add("Fechou" to a.porEtapa.fechou.toString())
                    add("Perdeu" to a.porEtapa.perdeu.toString())
                    add("Em andamento (novo + contatado + negociação)" to a.emAndamento.toString())
                    a.diasAteVenda?.media?.let { add("Dias até a venda (média)" to formatNumber(it)) }
                    a.diasAteVenda?.mediana?.let { add("Dias até a venda (mediana)" to formatNumber(it)) }
                }
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                    linhas.forEachIndexed { i, (rotulo, valor) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clearAndSetSemantics { contentDescription = "$rotulo: $valor" },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(valor, style = MaterialTheme.typography.labelLarge)
                        }
                        if (i != linhas.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            if (a.funil.isNotEmpty()) {
                FunilCard(a.funil)
                Text("Funil acumulado: pela etapa mais avançada que cada lead já alcançou.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (a.porOrigem.isNotEmpty()) OrigemCard(a.porOrigem.map { it.rotulo to it.total }, a.captados, semOrigem = 0)
            EvolutionCard(
                title = "Leads captados",
                unit = "leads por ${if (a.evolucao.size >= 2) "intervalo" else "período"}, pela data de captura",
                period = periodo,
                points = a.evolucao.map { ChartPoint(it.rotulo, it.captados.toFloat()) },
            )

            Text("Vendas e recebimentos no período", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            SynergyCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val stats: @Composable (Modifier) -> Unit = { m ->
                        Stat("Vendas fechadas", a.vendasNoPeriodo.toString(), m)
                        Stat("Vendido", formatMoney(a.vendidoCentavos), m, MaterialTheme.colorScheme.primary)
                        Stat("Recebido", formatMoney(a.recebidoCentavos), m, MaterialTheme.colorScheme.primary)
                    }
                    if (isLargeFont() || androidx.compose.ui.platform.LocalConfiguration.current.fontScale > 1.15f) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { stats(Modifier) }
                    else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { stats(Modifier.weight(1f)) }
                    Text(
                        "Vendido: vendas fechadas dentro do período, qualquer que seja a data de captura do lead. Recebido: pagamentos com data dentro do período.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Vendido: ${variacaoTexto(d.comparacao?.vendido)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Recebido: ${variacaoTexto(d.comparacao?.recebido)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Receita dos leads deste período que já foram ganhos: ${formatMoney(a.receitaCoorteCentavos)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (a.evolucao.size >= 2 && a.evolucao.any { it.ganhos > 0 }) {
                EvolutionCard("Vendas fechadas", "vendas por intervalo, pela data de fechamento", periodo, a.evolucao.map { ChartPoint(it.rotulo, it.ganhos.toFloat()) })
            }
            if (a.evolucao.size >= 2 && a.evolucao.any { it.vendidoCentavos > 0 }) {
                EvolutionCard(
                    "Valor vendido", "reais por intervalo, pela data de fechamento", periodo,
                    a.evolucao.map { ChartPoint(it.rotulo, it.vendidoCentavos / 100f) }, format = { formatMoney((it * 100).toLong()) },
                )
            }
        }

        d.metas.filter { it.metas.isNotEmpty() }.forEach { MetasSection("Metas — ${it.aplicacao}", it.metas) }
        SiteCard(d.site)
    }
}

/** Acessos e cliques do site: `null` = sem dado (nunca zero); sessões não são pessoas únicas. */
@Composable
private fun SiteCard(site: SiteResumoDto?) {
    if (site == null) {
        InfoNote("Acessos e cliques indisponíveis", "Não foi possível ler a coleta do site agora.")
        return
    }
    val coleta = site.coleta
    if (coleta == null || !coleta.ativa) {
        InfoNote("Acessos e cliques ainda sem dados", coleta?.observacao ?: "A coleta do site ainda não recebeu eventos para esta empresa.")
        return
    }
    fun n(v: Long?) = v?.let { formatNumber(it.toDouble()) } ?: "Sem dado"
    SynergyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Acessos e cliques do site", style = MaterialTheme.typography.titleMedium)
            Stat("Páginas vistas", n(site.acessos?.paginasVistas))
            Stat("Sessões", n(site.acessos?.sessoes))
            Stat("Cliques", n(site.cliques?.total))
            site.cliques?.porAlvo?.takeIf { it.isNotEmpty() }?.forEach { Text("${it.alvo}: ${formatNumber(it.total.toDouble())}", style = MaterialTheme.typography.bodySmall) }
            if (coleta.parcial) coleta.observacao?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                site.acessos?.sessoesSignificam ?: "Sessões são visitas por dia no navegador; não são pessoas únicas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
