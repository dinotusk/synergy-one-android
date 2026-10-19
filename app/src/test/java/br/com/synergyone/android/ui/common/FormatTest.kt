package br.com.synergyone.android.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    @Test
    fun `modulos viram os nomes do site e chave desconhecida nunca vaza crua`() {
        assertEquals("Pré-análise", moduloLabel("preAnalise"))
        assertEquals("Leads e funil", moduloLabel("leads"))
        assertEquals("Mídias e relatórios", moduloLabel("midias"))
        assertEquals("API e webhooks", moduloLabel("api"))
        assertEquals("Novo recurso extra", moduloLabel("novoRecursoExtra"))
    }

    @Test
    fun `origem real vence o canal e legado nunca aparece`() {
        assertEquals("Dra Bianca", origemLabel("Dra Bianca", "legado"))
        assertEquals("Instagram", origemLabel(null, "instagram"))
        assertEquals("Google ads", origemLabel("", "google_ads"))
        assertNull(origemLabel(null, "legado"))
        assertNull(origemLabel("Legado", "legado"))
        assertNull(origemLabel(null, null))
    }

    @Test
    fun `periodo curto omite o ano quando e o mesmo`() {
        assertEquals("01/09 – 30/09", formatPeriodShort("2026-09-01", "2026-09-30"))
        assertEquals("15/12/2025 – 10/01/2026", formatPeriodShort("2025-12-15", "2026-01-10"))
    }
}
