package br.com.synergyone.android.ui.termometro

import java.time.LocalDate
import java.time.ZoneId

/** Período escolhido para o painel da equipe. `desde`/`ate` em `AAAA-MM-DD` (fim incluso). */
data class PeriodRange(val label: String, val desde: String, val ate: String)

/** Datas sempre no horário de Brasília, o mesmo critério da API. */
fun hojeBrasilia(): LocalDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"))

fun periodPresets(hoje: LocalDate = hojeBrasilia()): List<PeriodRange> {
    fun last(days: Long, label: String) = PeriodRange(label, hoje.minusDays(days - 1).toString(), hoje.toString())
    val primeiroDoMes = hoje.withDayOfMonth(1)
    val mesAnterior = primeiroDoMes.minusMonths(1)
    return listOf(
        last(7, "Últimos 7 dias"),
        last(30, "Últimos 30 dias"),
        last(90, "Últimos 90 dias"),
        PeriodRange("Este mês", primeiroDoMes.toString(), hoje.toString()),
        PeriodRange("Mês anterior", mesAnterior.toString(), mesAnterior.withDayOfMonth(mesAnterior.lengthOfMonth()).toString()),
    )
}
