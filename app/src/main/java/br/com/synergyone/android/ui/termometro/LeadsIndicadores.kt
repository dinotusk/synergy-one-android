package br.com.synergyone.android.ui.termometro

import br.com.synergyone.android.data.api.EvolucaoDto
import br.com.synergyone.android.data.api.FunilDto
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.ui.common.origemLabel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/*
 * Dois conjuntos de números, **nunca misturados** (o cliente não tem rota de resultados ao vivo; tudo vem de `GET /leads`):
 *
 *  1. Leads captados no período (base: `capturadoEm`): quantos entraram, onde estão hoje, de onde vieram, conversão.
 *  2. Vendas realizadas no período (base: `vendaFechadaEm`): quantas e quanto, por data de fechamento.
 *
 * Mesma regra de `calcularResultados` do site. Diferença conhecida: o funil do site é acumulado (`etapaMaxima`), que `/leads`
 * não entrega; aqui é a etapa atual de cada lead.
 *
 * Todas as datas no horário de America/Sao_Paulo, o mesmo critério da API.
 */

private val BRASILIA = ZoneId.of("America/Sao_Paulo")
private val ETAPAS_ABERTAS = listOf("qualificado" to "Qualificado", "contato" to "Em contato", "proposta" to "Proposta", "negociacao" to "Negociação")

/** Aceita instante ISO (`capturadoEm`) ou data pura (`vendaFechadaEm`: `AAAA-MM-DD`). */
internal fun dataBrasilia(iso: String?): LocalDate? {
    if (iso.isNullOrBlank()) return null
    return runCatching { Instant.parse(iso).atZone(BRASILIA).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
}

/** Leads **captados** no período, classificados pela etapa **atual**. */
data class IndicadoresDaCoorte(
    val total: Int,
    val ganhos: Int,
    val perdidos: Int,
    val emAndamento: Int,
    /** Ganhos ÷ leads captados no período (mesma coorte). `null` sem leads. Não é "vendas do período ÷ leads". */
    val conversao: Double?,
    val funil: List<FunilDto>,
    /** Origem real (nunca "legado"), da maior para a menor. */
    val origens: List<Pair<String, Int>>,
    /** Origem desconhecida continua desconhecida: não vira canal nem "legado". */
    val semOrigem: Int,
    val captadosPorIntervalo: List<EvolucaoDto>,
    /** Como no site: dias da captura ao fechamento dos **ganhos desta coorte** (que tenham data de fechamento). */
    val diasMedia: Double?,
    val diasMediana: Double?,
    val diasAmostras: Int,
)

fun indicadoresDaCoorte(leads: List<LeadDto>, desde: LocalDate, ate: LocalDate): IndicadoresDaCoorte {
    val coorte = leads.mapNotNull { l -> dataBrasilia(l.capturadoEm)?.takeIf { !it.isBefore(desde) && !it.isAfter(ate) }?.let { l to it } }
    val total = coorte.size
    val ganhos = coorte.count { it.first.etapa == "ganho" }
    val perdidos = coorte.count { it.first.etapa == "perdido" }
    val porEtapa = coorte.groupingBy { it.first.etapa }.eachCount()
    val funil = if (total == 0) emptyList() else buildList {
        add(FunilDto("captado", "Total captado", total))
        porEtapa["captado"]?.let { add(FunilDto("sem_avanco", "Ainda sem avanço", it)) }
        ETAPAS_ABERTAS.forEach { (e, rotulo) -> porEtapa[e]?.let { add(FunilDto(e, rotulo, it)) } }
        if (ganhos > 0) add(FunilDto("ganho", "Ganhos", ganhos))
        if (perdidos > 0) add(FunilDto("perdido", "Perdidos", perdidos))
    }
    val dias = coorte.filter { it.first.etapa == "ganho" }
        .mapNotNull { (l, captado) -> dataBrasilia(l.vendaFechadaEm)?.let { ChronoUnit.DAYS.between(captado, it).toDouble().coerceAtLeast(0.0) } }
        .sorted()
    val origens = coorte.mapNotNull { origemLabel(it.first.origem, it.first.canal) }.groupingBy { it }.eachCount()
    return IndicadoresDaCoorte(
        total = total,
        ganhos = ganhos,
        perdidos = perdidos,
        emAndamento = total - ganhos - perdidos,
        conversao = if (total > 0) ganhos.toDouble() / total else null,
        funil = funil,
        origens = origens.entries.sortedByDescending { it.value }.map { it.key to it.value },
        semOrigem = total - origens.values.sum(),
        captadosPorIntervalo = serie<LocalDate>(desde, ate, coorte.map { it.second }) { g -> EvolucaoDto(captados = g.size) },
        diasMedia = if (dias.isEmpty()) null else dias.average(),
        diasMediana = mediana(dias),
        diasAmostras = dias.size,
    )
}

/** Vendas **fechadas** no período (`vendaFechadaEm`), qualquer que seja a data de captura. */
data class VendasDoPeriodo(
    val vendas: Int,
    val valorCentavos: Long,
    /** Vendas sem valor registrado: ficam fora do valor vendido (que é, então, um mínimo). */
    val semValor: Int,
    val porIntervalo: List<EvolucaoDto>,
)

/** [ganhos] = todos os leads em etapa `ganho` (`GET /leads?etapa=ganho`). Sem data de fechamento não entra: conta em [semDataFechamento]. */
fun vendasDoPeriodo(ganhos: List<LeadDto>, desde: LocalDate, ate: LocalDate): VendasDoPeriodo {
    val no = ganhos.filter { it.etapa == "ganho" }.mapNotNull { l -> dataBrasilia(l.vendaFechadaEm)?.takeIf { !it.isBefore(desde) && !it.isAfter(ate) }?.let { l to it } }
    // A série fica por data de fechamento: pares (data, venda) agrupados no mesmo intervalo.
    val serie = serie(desde, ate, no.map { it.second }, no.map { it.first }) { g -> EvolucaoDto(ganhos = g.size, vendidoCentavos = g.sumOf { it.vendaCentavos ?: 0L }) }
    return VendasDoPeriodo(
        vendas = no.size,
        valorCentavos = no.sumOf { it.first.vendaCentavos ?: 0L },
        semValor = no.count { (it.first.vendaCentavos ?: 0L) <= 0L },
        porIntervalo = serie,
    )
}

private fun mediana(ordenados: List<Double>): Double? = when {
    ordenados.isEmpty() -> null
    ordenados.size % 2 == 1 -> ordenados[ordenados.size / 2]
    else -> (ordenados[ordenados.size / 2 - 1] + ordenados[ordenados.size / 2]) / 2
}

/** Ganhos sem data de fechamento (venda antiga/importada): não dá para dizer em que período ocorreram. */
fun ganhosSemDataFechamento(ganhos: List<LeadDto>): Int = ganhos.count { it.etapa == "ganho" && dataBrasilia(it.vendaFechadaEm) == null }

/** Dia a dia até 31 dias; semanas (segunda a domingo) acima disso. Intervalos sem registro valem 0: a lista completa é a fonte. */
private fun <T> serie(desde: LocalDate, ate: LocalDate, datas: List<LocalDate>, itens: List<T>? = null, montar: (List<T>) -> EvolucaoDto): List<EvolucaoDto> {
    val diario = ChronoUnit.DAYS.between(desde, ate) < 31
    fun inicio(d: LocalDate) = if (diario) d else d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { if (it.isBefore(desde)) desde else it }
    @Suppress("UNCHECKED_CAST")
    val base: List<T> = itens ?: (datas as List<T>)
    val grupos = base.indices.groupBy({ inicio(datas[it]) }, { base[it] })
    val pontos = mutableListOf<EvolucaoDto>()
    var d = desde
    while (!d.isAfter(ate)) {
        val i = inicio(d)
        if (pontos.isEmpty() || pontos.last().chave != i.toString()) {
            pontos += montar(grupos[i].orEmpty()).copy(chave = i.toString(), rotulo = "%02d/%02d".format(i.dayOfMonth, i.monthValue))
        }
        d = d.plusDays(1)
    }
    return pontos
}
