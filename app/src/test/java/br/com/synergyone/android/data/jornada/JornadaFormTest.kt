package br.com.synergyone.android.data.jornada

import br.com.synergyone.android.data.api.JornadaLeadDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JornadaFormTest {
    @Test
    fun `dinheiro digitado vira decimal canonico`() {
        assertEquals("1500.00", parseMoneyInput("1500"))
        assertEquals("1500.50", parseMoneyInput("1500.5"))
        assertEquals("1500.00", parseMoneyInput("1.500,00"))
        assertEquals("1500.50", parseMoneyInput("R$ 1.500,50"))
        assertEquals("1500000.00", parseMoneyInput("1.500.000"))
        assertEquals("0.99", parseMoneyInput("0,99"))
        assertNull(parseMoneyInput(""))
        assertNull(parseMoneyInput("abc"))
        assertNull(parseMoneyInput("-5"))
    }

    @Test
    fun `centavos da API voltam para o campo`() {
        assertEquals("1500.00", centavosToInput(150000))
        assertEquals("0.05", centavosToInput(5))
        assertEquals("", centavosToInput(null))
    }

    @Test
    fun `ganho exige valor positivo e data`() {
        val base = JornadaForm(etapa = "ganho")
        assertEquals("vendaValor", base.validate()?.campo)
        assertEquals("vendaValor", base.copy(vendaValor = "0").validate()?.campo)
        assertEquals("vendaFechadaEm", base.copy(vendaValor = "1500").validate()?.campo)
        assertNull(base.copy(vendaValor = "1500", vendaFechadaEm = "2026-10-10").validate())
    }

    @Test
    fun `perdido exige motivo ate 300 e data`() {
        val base = JornadaForm(etapa = "perdido")
        assertEquals("motivoPerda", base.validate()?.campo)
        assertEquals("motivoPerda", base.copy(motivoPerda = "x".repeat(301), perdidoEm = "2026-10-10").validate()?.campo)
        assertEquals("perdidoEm", base.copy(motivoPerda = "Sem orçamento").validate()?.campo)
        assertNull(base.copy(motivoPerda = "Sem orçamento", perdidoEm = "2026-10-10").validate())
    }

    @Test
    fun `outras etapas nao exigem nada alem de valor valido`() {
        assertNull(JornadaForm(etapa = "contato").validate())
        assertEquals("propostaValor", JornadaForm(etapa = "proposta", propostaValor = "abc").validate()?.campo)
    }

    @Test
    fun `o estado completo limpa os campos que nao pertencem a etapa`() {
        val f = JornadaForm(
            etapa = "contato", vendaValor = "1500", vendaFechadaEm = "2026-10-10",
            motivoPerda = "x", perdidoEm = "2026-10-11", clienteEmpresaId = 4, propostaValor = "900",
        )
        val r = f.toRequest()
        assertNull(r.vendaValor); assertNull(r.vendaFechadaEm); assertNull(r.motivoPerda); assertNull(r.perdidoEm); assertNull(r.clienteEmpresaId)
        assertEquals("900.00", r.propostaValor)

        val ganho = JornadaForm(etapa = "ganho", vendaValor = "1.500,00", vendaFechadaEm = "2026-10-10", clienteEmpresaId = 4, motivoPerda = "resto").toRequest()
        assertEquals("1500.00", ganho.vendaValor); assertEquals(4L, ganho.clienteEmpresaId); assertNull(ganho.motivoPerda)
    }

    @Test
    fun `formulario nasce do estado atual do lead`() {
        val f = JornadaForm.from(JornadaLeadDto(id = 1, etapa = "proposta", propostaCentavos = 150000, responsavelId = "r1", campanhaId = 7))
        assertEquals("proposta", f.etapa); assertEquals("1500.00", f.propostaValor)
        assertEquals("r1", f.responsavelId); assertEquals(7L, f.campanhaId)
    }
}
