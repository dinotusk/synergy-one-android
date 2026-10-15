package br.com.synergyone.android.ui.termometro

import br.com.synergyone.android.data.api.LeadDto
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class LeadsIndicadoresTest {
    private fun lead(id: Long, em: String, etapa: String = "captado", origem: String? = null, canal: String? = null, venda: Long? = null, fechada: String? = null) =
        LeadDto(id = id, capturadoEm = em, etapa = etapa, origem = origem, canal = canal, vendaCentavos = venda, vendaFechadaEm = fechada)

    private val desde = LocalDate.parse("2026-10-01")
    private val ate = LocalDate.parse("2026-10-10")

    @Test fun `periodo sem leads nao inventa numeros`() {
        val r = indicadoresDeLeads(emptyList(), desde, ate)
        assertEquals(0, r.resultados.totalLeads)
        assertNull(r.resultados.conversao)
        assertNull(r.resultados.diasAteVenda)
        assertTrue(r.resultados.funil.isEmpty())
        assertTrue(r.origens.isEmpty())
    }

    @Test fun `conta so a coorte do periodo e classifica pela etapa atual`() {
        val leads = listOf(
            lead(1, "2026-10-02T15:00:00.000Z", "ganho", venda = 150_000, fechada = "2026-10-06T15:00:00.000Z"),
            lead(2, "2026-10-03T15:00:00.000Z", "perdido"),
            lead(3, "2026-10-04T15:00:00.000Z", "proposta"),
            lead(4, "2026-09-20T15:00:00.000Z", "ganho", venda = 999),
            lead(5, "2026-10-11T15:00:00.000Z"),
        )
        val r = indicadoresDeLeads(leads, desde, ate).resultados
        assertEquals(3, r.totalLeads)
        assertEquals(1, r.ganhos)
        assertEquals(1, r.perdidos)
        assertEquals(1, r.emAndamento)
        assertEquals(1.0 / 3, r.conversao!!, 1e-9)
        assertEquals(150_000L, r.vendidoCentavos)
        assertEquals(4.0, r.diasAteVenda!!.media!!, 1e-9)
        assertEquals(10, r.evolucao.size)
        assertEquals(3, r.evolucao.sumOf { it.captados })
    }

    @Test fun `origem real nunca mostra legado`() {
        val leads = listOf(
            lead(1, "2026-10-02T15:00:00.000Z", origem = "Dra Bianca", canal = "legado"),
            lead(2, "2026-10-02T16:00:00.000Z", origem = "legado", canal = "instagram"),
            lead(3, "2026-10-02T17:00:00.000Z", origem = "legado", canal = "legado"),
        )
        val r = indicadoresDeLeads(leads, desde, ate)
        assertEquals(listOf("Dra Bianca" to 1, "Instagram" to 1), r.origens.sortedBy { it.first })
        assertEquals(1, r.semOrigem)
    }

    @Test fun `periodo longo agrupa por semana`() {
        val r = indicadoresDeLeads(listOf(lead(1, "2026-09-15T15:00:00.000Z")), LocalDate.parse("2026-08-17"), LocalDate.parse("2026-10-10"))
        assertTrue(r.resultados.evolucao.size in 8..9)
        assertEquals(1, r.resultados.evolucao.sumOf { it.captados })
    }

    @Test fun `data de captura usa o dia de Brasilia`() {
        // 01/10 02:00 UTC = 30/09 23:00 em Brasília: fora do período que começa em 01/10.
        assertEquals(0, indicadoresDeLeads(listOf(lead(1, "2026-10-01T02:00:00.000Z")), desde, ate).resultados.totalLeads)
    }
}
