package br.com.synergyone.android.ui.termometro

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.api.LeadsPage
import br.com.synergyone.android.data.lead.LeadRepository
import br.com.synergyone.android.data.lead.captadosDesde
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeadsIndicadoresTest {
    private fun lead(id: Long, em: String, etapa: String = "captado", origem: String? = null, canal: String? = null, venda: Long? = null, fechada: String? = null) =
        LeadDto(id = id, capturadoEm = em, etapa = etapa, origem = origem, canal = canal, vendaCentavos = venda, vendaFechadaEm = fechada)

    private val desde = LocalDate.parse("2026-10-01")
    private val ate = LocalDate.parse("2026-10-10")

    @Test fun `periodo sem leads nao inventa numeros`() {
        val c = indicadoresDaCoorte(emptyList(), desde, ate)
        assertEquals(0, c.total)
        assertNull(c.conversao)
        assertNull(c.diasMedia)
        assertTrue(c.funil.isEmpty())
        assertTrue(c.origens.isEmpty())
    }

    @Test fun `coorte conta so captados no periodo e conversao usa a mesma base`() {
        val leads = listOf(
            lead(1, "2026-10-02T15:00:00.000Z", "ganho", venda = 150_000, fechada = "2026-10-06"),
            lead(2, "2026-10-03T15:00:00.000Z", "perdido"),
            lead(3, "2026-10-04T15:00:00.000Z", "proposta"),
            lead(4, "2026-09-20T15:00:00.000Z", "ganho", venda = 999, fechada = "2026-10-05"), // captado antes: fora da coorte
            lead(5, "2026-10-11T15:00:00.000Z"),
        )
        val c = indicadoresDaCoorte(leads, desde, ate)
        assertEquals(3, c.total)
        assertEquals(1, c.ganhos)
        assertEquals(1, c.perdidos)
        assertEquals(1, c.emAndamento)
        assertEquals(1.0 / 3, c.conversao!!, 1e-9)
        assertEquals(4.0, c.diasMedia!!, 1e-9)
        assertEquals(1, c.diasAmostras)
        assertEquals(10, c.captadosPorIntervalo.size)
        assertEquals(3, c.captadosPorIntervalo.sumOf { it.captados })
    }

    @Test fun `vendas contam pela data de fechamento e nao pela de captura`() {
        val ganhos = listOf(
            lead(1, "2026-10-02T15:00:00.000Z", "ganho", venda = 150_000, fechada = "2026-10-06"),
            lead(4, "2026-09-20T15:00:00.000Z", "ganho", venda = 200_000, fechada = "2026-10-05"), // captado antes, vendido no periodo
            lead(6, "2026-10-03T15:00:00.000Z", "ganho", venda = 700_000, fechada = "2026-10-20"), // captado no periodo, vendido depois
            lead(7, "2026-10-03T15:00:00.000Z", "ganho", venda = 100_000, fechada = null),
        )
        val v = vendasDoPeriodo(ganhos, desde, ate)
        assertEquals(2, v.vendas)
        assertEquals(350_000L, v.valorCentavos)
        assertEquals(0, v.semValor)
        assertEquals(1, ganhosSemDataFechamento(ganhos))
        assertEquals(2, v.porIntervalo.sumOf { it.ganhos })
    }

    @Test fun `venda sem valor e informada e nao inventa valor`() {
        val v = vendasDoPeriodo(listOf(lead(1, "2026-10-02T15:00:00.000Z", "ganho", fechada = "2026-10-06")), desde, ate)
        assertEquals(1, v.vendas)
        assertEquals(0L, v.valorCentavos)
        assertEquals(1, v.semValor)
    }

    @Test fun `origem real nunca mostra legado e desconhecida continua desconhecida`() {
        val leads = listOf(
            lead(1, "2026-10-02T15:00:00.000Z", origem = "Dra Bianca", canal = "legado"),
            lead(2, "2026-10-02T16:00:00.000Z", origem = "legado", canal = "instagram"),
            lead(3, "2026-10-02T17:00:00.000Z", origem = "legado", canal = "legado"),
        )
        val c = indicadoresDaCoorte(leads, desde, ate)
        assertEquals(listOf("Dra Bianca" to 1, "Instagram" to 1), c.origens.sortedBy { it.first })
        assertEquals(1, c.semOrigem)
        assertTrue(c.origens.none { it.first.equals("legado", ignoreCase = true) })
    }

    @Test fun `periodo longo agrupa por semana`() {
        val c = indicadoresDaCoorte(listOf(lead(1, "2026-09-15T15:00:00.000Z")), LocalDate.parse("2026-08-17"), LocalDate.parse("2026-10-10"))
        assertTrue(c.captadosPorIntervalo.size in 8..9)
        assertEquals(1, c.captadosPorIntervalo.sumOf { it.captados })
    }

    @Test fun `data de captura usa o dia de Brasilia`() {
        // 01/10 02:00 UTC = 30/09 23:00 em Brasília: fora do período que começa em 01/10.
        assertEquals(0, indicadoresDaCoorte(listOf(lead(1, "2026-10-01T02:00:00.000Z")), desde, ate).total)
    }

    private class Paginado(private val paginas: Int) : LeadRepository {
        var chamadas = 0
        override fun cached(id: Long): LeadDto? = null
        override suspend fun pagina(empresaId: Long?, etapa: String?, cursor: String?, desde: String?, limite: Int): ApiResult<LeadsPage> {
            chamadas++
            return ApiResult.Success(LeadsPage(listOf(LeadDto(id = chamadas.toLong())), total = paginas * 100, proximoCursor = if (chamadas < paginas) "c$chamadas" else null))
        }
    }

    @Test fun `ate o teto de paginas a lista e completa`() = runBlocking {
        val r = Paginado(10).captadosDesde(1, "2026-10-01") as ApiResult.Success
        assertTrue(r.data.completo)
        assertEquals(10, r.data.leads.size)
    }

    @Test fun `acima do teto de 1000 registros a lista e marcada como incompleta`() = runBlocking {
        val repo = Paginado(11)
        val r = repo.captadosDesde(1, "2026-10-01") as ApiResult.Success
        assertFalse(r.data.completo)
        assertEquals(10, repo.chamadas)
        assertEquals(1100, r.data.total)
    }
}
