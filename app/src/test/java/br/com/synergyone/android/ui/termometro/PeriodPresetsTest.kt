package br.com.synergyone.android.ui.termometro

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodPresetsTest {
    private fun presets(hoje: String) = periodPresets(LocalDate.parse(hoje)).associateBy { it.label }

    @Test
    fun `ultimos N dias incluem hoje e contam N dias`() {
        val p = presets("2026-10-15")
        assertEquals("2026-10-09" to "2026-10-15", p["Últimos 7 dias"]!!.let { it.desde to it.ate })
        assertEquals("2026-09-16" to "2026-10-15", p["Últimos 30 dias"]!!.let { it.desde to it.ate })
    }

    @Test
    fun `este mes e mes anterior`() {
        val p = presets("2026-10-15")
        assertEquals("2026-10-01" to "2026-10-15", p["Este mês"]!!.let { it.desde to it.ate })
        assertEquals("2026-09-01" to "2026-09-30", p["Mês anterior"]!!.let { it.desde to it.ate })
    }

    @Test
    fun `mes anterior na virada de ano e em fevereiro bissexto`() {
        assertEquals("2025-12-01" to "2025-12-31", presets("2026-01-10")["Mês anterior"]!!.let { it.desde to it.ate })
        assertEquals("2028-02-01" to "2028-02-29", presets("2028-03-05")["Mês anterior"]!!.let { it.desde to it.ate })
    }
}
