package br.com.synergyone.android.ui.termometro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.lead.LeadRepository
import br.com.synergyone.android.data.lead.LeadsDesde
import br.com.synergyone.android.data.lead.MAX_PAGINAS
import br.com.synergyone.android.data.lead.captadosDesde
import br.com.synergyone.android.ui.common.InfoNote
import br.com.synergyone.android.ui.common.LoadState
import br.com.synergyone.android.ui.common.LoadStateContent
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.common.formatMoney
import br.com.synergyone.android.ui.common.formatNumber
import br.com.synergyone.android.ui.common.formatPeriod
import br.com.synergyone.android.ui.common.formatPercent
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.ui.components.ChartPoint
import br.com.synergyone.android.ui.components.EvolutionCard
import br.com.synergyone.android.ui.components.SectionTitle
import br.com.synergyone.android.ui.components.SynergyCard
import java.time.LocalDate

/**
 * Cliente: indicadores **atuais** do período, calculados agora a partir de `GET /leads`. Não são os
 * números congelados de uma avaliação publicada. Dois blocos, com bases diferentes e títulos próprios:
 * leads captados no período e vendas fechadas no período.
 */
@Composable
fun IndicadoresAtuais(repository: LeadRepository, empresaId: Long?, intervalo: PeriodRange, onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    val periodo = formatPeriod(intervalo.desde, intervalo.ate)
    val desde = LocalDate.parse(intervalo.desde)
    val ate = LocalDate.parse(intervalo.ate)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Indicadores atuais do período", eyebrow = periodo)
        InfoNote(
            "Calculados agora, não congelados",
            "Estes números vêm dos seus leads neste momento, no horário de Brasília. Mudam conforme os leads avançam e não são os números congelados de uma avaliação publicada.",
        )

        val coorte = rememberLoader("ind-coorte-$empresaId-${intervalo.desde}") { repository.captadosDesde(empresaId, intervalo.desde) }
        val estadoCoorte by coorte.state.collectAsState()
        Text("Leads captados no período", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        LoadStateContent(estadoCoorte, onRetry = coorte::reload, onSignIn = onSignIn) { d ->
            if (!d.completo) LimiteNote(d, intervalo) else CoorteBlock(d, desde, ate, periodo)
        }

        val ganhos = rememberLoader("ind-ganhos-$empresaId") { repository.captadosDesde(empresaId, desde = null, etapa = "ganho") }
        val estadoGanhos by ganhos.state.collectAsState()
        Text("Vendas fechadas no período", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        LoadStateContent(estadoGanhos, onRetry = ganhos::reload, onSignIn = onSignIn) { d ->
            if (!d.completo) LimiteNote(d, null) else VendasBlock(d, desde, ate, periodo)
        }
    }
}

/** Limite de leitura: nada parcial é apresentado como resultado completo. */
@Composable
private fun LimiteNote(d: LeadsDesde, intervalo: PeriodRange?) {
    val max = formatNumber((MAX_PAGINAS * 100).toDouble())
    val base = if (intervalo != null) "Há ${formatNumber(d.total.toDouble())} leads captados desde ${formatDate(intervalo.desde)}" else "Há ${formatNumber(d.total.toDouble())} vendas registradas"
    InfoNote(
        "Volume acima do que o app calcula",
        "$base, mais do que os $max registros que o app lê. Para não mostrar totais, percentuais ou gráficos parciais como se fossem completos, estes números não são exibidos" +
            (if (intervalo != null) ". Escolha um período menor (o filtro vale pela data de captura)." else ".") +
            " Para períodos longos, o backend precisa entregar os números já agregados.",
    )
}

@Composable
private fun CoorteBlock(d: LeadsDesde, desde: LocalDate, ate: LocalDate, periodo: String) {
    val c = indicadoresDaCoorte(d.leads, desde, ate)
    if (c.total == 0) {
        InfoNote("Sem leads captados neste período", "Nenhum lead entrou entre estas datas. Tente outro período.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val stats: @Composable (Modifier) -> Unit = { m ->
                    Stat("Leads captados", c.total.toString(), m)
                    Stat("Já ganhos", c.ganhos.toString(), m)
                    Stat("Conversão", formatPercent(c.conversao), m)
                }
                if (isLargeFont()) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { stats(Modifier) }
                else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { stats(Modifier.weight(1f)) }
                Text(
                    "Conversão = leads captados no período que já foram ganhos ÷ leads captados no período. Não é o total de vendas fechadas no período.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                c.diasMedia?.let {
                    Text(
                        "Tempo até a venda: ${formatNumber(it)} dias em média, dos leads deste período já ganhos (captura → fechamento, ${c.diasAmostras} venda${if (c.diasAmostras == 1) "" else "s"})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    "Perdidos: ${c.perdidos} • Em andamento: ${c.emAndamento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FunilCard(c.funil)
        Text("Etapa em que cada lead está hoje; não é o funil acumulado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (c.origens.isNotEmpty()) OrigemCard(c.origens, c.total, c.semOrigem)
        else InfoNote("Origem não informada", "Nenhum lead deste período traz origem registrada.")
        EvolutionCard(
            title = "Leads captados",
            unit = "leads por ${if (c.captadosPorIntervalo.size >= 2) "intervalo" else "período"}, pela data de captura",
            period = periodo,
            points = c.captadosPorIntervalo.map { ChartPoint(it.rotulo, it.captados.toFloat()) },
        )
    }
}

@Composable
private fun VendasBlock(d: LeadsDesde, desde: LocalDate, ate: LocalDate, periodo: String) {
    val v = vendasDoPeriodo(d.leads, desde, ate)
    val semData = ganhosSemDataFechamento(d.leads)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (v.vendas == 0) {
            InfoNote("Nenhuma venda fechada neste período", "Contam as vendas pela data de fechamento, mesmo as de leads captados antes.")
        } else {
            SynergyCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val stats: @Composable (Modifier) -> Unit = { m ->
                        Stat("Vendas", v.vendas.toString(), m)
                        Stat("Valor vendido", formatMoney(v.valorCentavos), m, MaterialTheme.colorScheme.primary)
                    }
                    if (isLargeFont() || androidx.compose.ui.platform.LocalConfiguration.current.fontScale > 1.15f) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { stats(Modifier) }
                    else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { stats(Modifier.weight(1f)) }
                    Text(
                        "Por data de fechamento da venda, qualquer que seja a data em que o lead foi captado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            EvolutionCard(
                title = "Vendas fechadas",
                unit = "vendas por intervalo, pela data de fechamento",
                period = periodo,
                points = v.porIntervalo.map { ChartPoint(it.rotulo, it.ganhos.toFloat()) },
            )
            if (v.valorCentavos > 0) {
                EvolutionCard(
                    title = "Valor vendido",
                    unit = "reais por intervalo, pela data de fechamento",
                    period = periodo,
                    points = v.porIntervalo.map { ChartPoint(it.rotulo, it.vendidoCentavos / 100f) },
                    format = { formatMoney((it * 100).toLong()) },
                )
            }
        }
        if (v.semValor > 0) {
            InfoNote("Vendas sem valor registrado", "${v.semValor} venda${if (v.semValor == 1) "" else "s"} do período não ${if (v.semValor == 1) "tem" else "têm"} valor; o valor vendido é, no mínimo, o mostrado.")
        }
        if (semData > 0) {
            InfoNote("Vendas sem data de fechamento", "$semData venda${if (semData == 1) "" else "s"} ganha${if (semData == 1) "" else "s"} não ${if (semData == 1) "tem" else "têm"} data de fechamento registrada; não dá para dizer em que período ${if (semData == 1) "ocorreu" else "ocorreram"}, então ficam de fora.")
        }
        InfoNote("Ainda não disponível no app", "Valores recebidos, acessos e cliques ainda não chegam à API do cliente.")
    }
}
