package br.com.synergyone.android.data.indicadores

import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.BibliotecaPage
import br.com.synergyone.android.data.api.IndicadoresDto
import br.com.synergyone.android.data.api.VariacaoDto
import br.com.synergyone.android.data.biblioteca.linkSeguro
import br.com.synergyone.android.ui.termometro.calculadoEmTexto
import br.com.synergyone.android.ui.termometro.variacaoTexto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Os JSON em `resources/contrato` são os exemplos gerados pelo backend (`docs/exemplos-api`), sem dado real. */
class ContratoIndicadoresBibliotecaTest {
    private fun texto(nome: String) = javaClass.getResource("/contrato/$nome")!!.readText()

    @Test
    fun `indicadores do exemplo 30d sao lidos sem perder numero`() {
        val d = AppApiClient.JSON.decodeFromString<IndicadoresDto>(texto("GET-indicadores-preset-30d.json"))
        assertEquals("2026-09-16", d.periodo?.de)
        assertEquals(20, d.atual.captados)
        assertEquals(4, d.atual.ganhos)
        assertEquals(0.2, d.atual.conversao!!, 0.0001)
        assertEquals(6, d.atual.funil.size)
        assertEquals(480050L, d.atual.receitaCoorteCentavos)
        assertEquals(600025L, d.atual.vendidoCentavos)
        assertEquals(250000L, d.atual.recebidoCentavos)
        assertEquals(5, d.atual.vendasNoPeriodo)
        assertEquals(25.0, d.comparacao?.leads?.valor!!, 0.0)
        assertEquals("pontos", d.comparacao?.conversaoPontos?.tipo)
        assertEquals("Origem não identificada", d.anterior?.resultados?.porOrigem?.first()?.rotulo)
        assertEquals(320L, d.site?.acessos?.paginasVistas)
        assertEquals(40.0, d.metas.first().metas.first().alvo!!, 0.0)
    }

    @Test
    fun `sem dados nao vira zero de conversao nem comparacao inventada`() {
        val d = AppApiClient.JSON.decodeFromString<IndicadoresDto>(texto("GET-indicadores-sem-dados.json"))
        assertEquals(0, d.atual.captados)
        assertNull(d.atual.conversao)
        assertEquals("Sem período anterior para comparar", variacaoTexto(d.comparacao?.leads))
    }

    @Test
    fun `variacao em percentual e em pontos, sem base nunca e zero`() {
        assertEquals("+25% em relação ao período anterior", variacaoTexto(VariacaoDto("percentual", 25.0)))
        assertEquals("−10% em relação ao período anterior", variacaoTexto(VariacaoDto("percentual", -10.0)))
        assertEquals("+1 pontos em relação ao período anterior", variacaoTexto(VariacaoDto("pontos", 1.0)))
        assertEquals("Sem período anterior para comparar", variacaoTexto(null))
    }

    @Test
    fun `calculadoEm aparece no horario de Brasilia`() {
        assertEquals("15/10 às 12:00", calculadoEmTexto("2026-10-15T15:00:00.000Z"))
        assertNull(calculadoEmTexto("lixo"))
    }

    @Test
    fun `parametros mandam preset ou de e ate, nunca os dois`() {
        assertEquals(mapOf("empresaId" to "3", "preset" to "30d"), parametros(3, PeriodoIndicadores(preset = "30d", de = "2026-09-17", ate = "2026-10-16")))
        assertEquals(mapOf("de" to "2026-10-10", "ate" to "2026-10-16"), parametros(null, PeriodoIndicadores(de = "2026-10-10", ate = "2026-10-16")))
        assertTrue(parametros(null, PeriodoIndicadores()).isEmpty())
    }

    @Test
    fun `biblioteca do cliente nao traz campos administrativos e lista vazia e valida`() {
        val cheia = AppApiClient.JSON.decodeFromString<BibliotecaPage>(texto("GET-biblioteca-cliente.json"))
        assertEquals(2, cheia.itens.size)
        assertNotNull(cheia.itens.first().url)
        assertNull(cheia.itens.first().visivelCliente)
        assertNull(cheia.itens.first().empresaNome)
        val vazia = AppApiClient.JSON.decodeFromString<BibliotecaPage>(texto("GET-biblioteca-cliente-vazia.json"))
        assertTrue(vazia.itens.isEmpty())
        assertEquals(0, vazia.total)
    }

    @Test
    fun `so links https abrem`() {
        assertEquals("https://drive.google.com/file/d/1/view", linkSeguro(" https://drive.google.com/file/d/1/view "))
        assertNull(linkSeguro("http://exemplo.com"))
        assertNull(linkSeguro("javascript:alert(1)"))
        assertNull(linkSeguro("intent://x#Intent;end"))
        assertNull(linkSeguro("file:///sdcard/a.pdf"))
        assertNull(linkSeguro("https://"))
        assertNull(linkSeguro(null))
    }
}
