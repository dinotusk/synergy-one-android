package br.com.synergyone.android.data.termometro

import br.com.synergyone.android.data.api.AvaliacaoEquipeDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvaliacaoFormTest {
    private val ok = AvaliacaoForm(
        empresaId = 3, periodoInicio = "2026-10-01", periodoFim = "2026-10-31", classificacao = "amarelo",
        justificativa = "Abaixo da meta.", feedback = "Vamos reforçar.",
    )

    @Test
    fun `formulario completo e valido`() = assertNull(ok.validate())

    @Test
    fun `campos obrigatorios e limites`() {
        assertEquals("classificacao", ok.copy(classificacao = null).validate()?.campo)
        assertEquals("classificacao", ok.copy(classificacao = "roxo").validate()?.campo)
        assertEquals("periodoInicio", ok.copy(periodoInicio = null).validate()?.campo)
        assertEquals("periodoFim", ok.copy(periodoFim = "2026-09-30").validate()?.campo)
        assertEquals("justificativa", ok.copy(justificativa = "  ").validate()?.campo)
        assertEquals("justificativa", ok.copy(justificativa = "x".repeat(2001)).validate()?.campo)
        assertEquals("feedback", ok.copy(feedback = "").validate()?.campo)
        assertEquals("feedback", ok.copy(feedback = "x".repeat(4001)).validate()?.campo)
        assertEquals("notaInterna", ok.copy(notaInterna = "x".repeat(2001)).validate()?.campo)
    }

    @Test
    fun `periodo de um dia so e aceito`() = assertNull(ok.copy(periodoFim = "2026-10-01").validate())

    @Test
    fun `nota interna vazia vai como nula e textos sao aparados`() {
        val r = ok.copy(notaInterna = "  ", justificativa = "  texto  ").toRequest()
        assertNull(r.notaInterna)
        assertEquals("texto", r.justificativa)
        assertEquals("Vamos reforçar.", r.feedback)
    }

    @Test
    fun `corrigir uma publicada copia o conteudo para um novo rascunho`() {
        val publicada = AvaliacaoEquipeDto(18, 3, 2, "2026-09-01", "2026-09-30", "verde", "Meta batida.", "Ótimo mês.", "interno", "publicada", "2026-10-02T10:00:00.000Z")
        val f = AvaliacaoForm.from(publicada, 3)
        assertEquals("verde", f.classificacao); assertEquals("Meta batida.", f.justificativa); assertEquals(2L, f.aplicacaoId)
        assertNull(f.validate())
    }
}
