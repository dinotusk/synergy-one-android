package br.com.synergyone.android.ui.termometro

import java.time.LocalDate
import java.time.ZoneId

/** Período escolhido para o painel da equipe. `desde`/`ate` em `AAAA-MM-DD` (fim incluso). */
/** [preset] (`30d`, `90d`, `mes`): quando existe, o servidor resolve o intervalo e o app não faz a conta. */
data class PeriodRange(val label: String, val desde: String, val ate: String, val preset: String? = null)

/** Datas sempre no horário de Brasília, o mesmo critério da API. */
fun hojeBrasilia(): LocalDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"))

fun periodPresets(hoje: LocalDate = hojeBrasilia()): List<PeriodRange> {
    fun last(days: Long, label: String, preset: String? = null) = PeriodRange(label, hoje.minusDays(days - 1).toString(), hoje.toString(), preset)
    val primeiroDoMes = hoje.withDayOfMonth(1)
    val mesAnterior = primeiroDoMes.minusMonths(1)
    return listOf(
        last(7, "Últimos 7 dias"),
        last(30, "Últimos 30 dias", "30d"),
        last(90, "Últimos 90 dias", "90d"),
        PeriodRange("Este mês", primeiroDoMes.toString(), hoje.toString(), "mes"),
        PeriodRange("Mês anterior", mesAnterior.toString(), mesAnterior.withDayOfMonth(mesAnterior.lengthOfMonth()).toString()),
    )
}
