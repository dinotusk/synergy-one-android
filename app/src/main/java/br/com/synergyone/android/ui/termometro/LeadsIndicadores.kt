package br.com.synergyone.android.ui.termometro

import br.com.synergyone.android.data.api.DiasAteVendaDto
import br.com.synergyone.android.data.api.EvolucaoDto
import br.com.synergyone.android.data.api.FunilDto
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.api.PeriodoDto
import br.com.synergyone.android.data.api.ResultadosDto
import br.com.synergyone.android.ui.common.origemLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Indicadores do período calculados só com `GET /leads` (o cliente não tem rota de resultados ao vivo). */
data class IndicadoresDeLeads(
    val resultados: ResultadosDto,
    /** Origem real (nunca "legado") e quantidade, da maior para a menor; sem origem conhecida fica fora. */
    val origens: List<Pair<String, Int>>,
    val semOrigem: Int,
)

private val BRASILIA = ZoneId.of("America/Sao_Paulo")
private val ETAPAS_ABERTAS = listOf("qualificado" to "Qualificado", "contato" to "Em contato", "proposta" to "Proposta", "negociacao" to "Negociação")

private fun dataBrasilia(iso: String?): LocalDate? =
    runCatching { Instant.parse(iso!!).atZone(BRASILIA).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso!!.take(10)) }.getOrNull()

/**
 * Coorte: leads **captados** entre [desde] e [ate] (inclusive), classificados pela etapa **atual**.
 * Sem recebimentos nem cliques (a API do cliente não os entrega): esses campos ficam ausentes, nunca zerados.
 */
fun indicadoresDeLeads(leads: List<LeadDto>, desde: LocalDate, ate: LocalDate): IndicadoresDeLeads {
    val coorte = leads.mapNotNull { l -> dataBrasilia(l.capturadoEm)?.takeIf { !it.isBefore(desde) && !it.isAfter(ate) }?.let { l to it } }
    val total = coorte.size
    val ganhos = coorte.count { it.first.etapa == "ganho" }
    val perdidos = coorte.count { it.first.etapa == "perdido" }

    val dias = coorte.filter { it.first.etapa == "ganho" }.mapNotNull { (l, captado) ->
        dataBrasilia(l.vendaFechadaEm)?.let { ChronoUnit.DAYS.between(captado, it).toDouble().coerceAtLeast(0.0) }
    }.sorted()
    val mediana = when {
        dias.isEmpty() -> null
        dias.size % 2 == 1 -> dias[dias.size / 2]
        else -> (dias[dias.size / 2 - 1] + dias[dias.size / 2]) / 2
    }

    val funil = if (total == 0) emptyList() else buildList {
        add(FunilDto("captado", "Total captado", total))
        val porEtapa = coorte.groupingBy { it.first.etapa }.eachCount()
        porEtapa["captado"]?.let { add(FunilDto("sem_avanco", "Ainda sem avanço", it)) }
        ETAPAS_ABERTAS.forEach { (e, rotulo) -> porEtapa[e]?.let { add(FunilDto(e, rotulo, it)) } }
        if (ganhos > 0) add(FunilDto("ganho", "Ganhos", ganhos))
        if (perdidos > 0) add(FunilDto("perdido", "Perdidos", perdidos))
    }

    val origensConhecidas = coorte.mapNotNull { origemLabel(it.first.origem, it.first.canal) }.groupingBy { it }.eachCount()

    return IndicadoresDeLeads(
        resultados = ResultadosDto(
            periodo = PeriodoDto(desde.toString(), ate.toString()),
            totalLeads = total,
            ganhos = ganhos,
            perdidos = perdidos,
            emAndamento = total - ganhos - perdidos,
            conversao = if (total > 0) ganhos.toDouble() / total else null,
            diasAteVenda = if (dias.isEmpty()) null else DiasAteVendaDto(dias.average(), mediana, dias.size),
            vendidoCentavos = coorte.filter { it.first.etapa == "ganho" }.sumOf { it.first.vendaCentavos ?: 0L },
            recebidoCentavos = 0,
            funil = funil,
            evolucao = serie(coorte, desde, ate),
        ),
        origens = origensConhecidas.entries.sortedByDescending { it.value }.map { it.key to it.value },
        semOrigem = coorte.size - origensConhecidas.values.sum(),
    )
}

/** Dia a dia até 31 dias; semanas (segunda a domingo) acima disso. Intervalos sem lead valem 0 (a lista é a fonte completa). */
private fun serie(coorte: List<Pair<LeadDto, LocalDate>>, desde: LocalDate, ate: LocalDate): List<EvolucaoDto> {
    val diario = ChronoUnit.DAYS.between(desde, ate) < 31
    fun inicio(d: LocalDate) = if (diario) d else d.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).let { if (it.isBefore(desde)) desde else it }
    val grupos = coorte.groupBy { inicio(it.second) }
    val pontos = mutableListOf<EvolucaoDto>()
    var d = desde
    while (!d.isAfter(ate)) {
        val i = inicio(d)
        if (pontos.isEmpty() || pontos.last().chave != i.toString()) {
            val g = grupos[i].orEmpty()
            pontos += EvolucaoDto(
                chave = i.toString(),
                rotulo = "%02d/%02d".format(i.dayOfMonth, i.monthValue),
                captados = g.size,
                ganhos = g.count { it.first.etapa == "ganho" },
                vendidoCentavos = g.filter { it.first.etapa == "ganho" }.sumOf { it.first.vendaCentavos ?: 0L },
            )
        }
        d = d.plusDays(1)
    }
    return pontos
}
