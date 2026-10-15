package br.com.synergyone.android.data.termometro

import br.com.synergyone.android.data.api.AvaliacaoEquipeDto
import br.com.synergyone.android.data.api.AvaliacaoRequest
import br.com.synergyone.android.data.jornada.FormError

/** Classificações aceitas pela API (`vermelho`, `amarelo`, `verde`, `azul`, `lilas`). */
val CLASSIFICACOES = listOf("vermelho", "amarelo", "verde", "azul", "lilas")

/**
 * Formulário de avaliação do Termômetro. As regras espelham o servidor: justificativa 1–2.000,
 * feedback 1–4.000, nota interna até 2.000 (ou nula), `periodoFim` ≥ `periodoInicio`.
 * O servidor continua sendo a autoridade; isto só evita ida e volta.
 */
data class AvaliacaoForm(
    val empresaId: Long,
    val aplicacaoId: Long? = null,
    val periodoInicio: String? = null,
    val periodoFim: String? = null,
    val classificacao: String? = null,
    val justificativa: String = "",
    val feedback: String = "",
    val notaInterna: String = "",
) {
    fun validate(): FormError? {
        if (classificacao !in CLASSIFICACOES) return FormError("classificacao", "Escolha a classificação.")
        if (periodoInicio.isNullOrBlank()) return FormError("periodoInicio", "Informe o início do período.")
        if (periodoFim.isNullOrBlank()) return FormError("periodoFim", "Informe o fim do período.")
        if (periodoFim < periodoInicio) return FormError("periodoFim", "O fim deve ser igual ou depois do início.")
        if (justificativa.isBlank()) return FormError("justificativa", "Escreva a justificativa.")
        if (justificativa.length > 2000) return FormError("justificativa", "A justificativa pode ter até 2.000 caracteres.")
        if (feedback.isBlank()) return FormError("feedback", "Escreva o feedback e os próximos passos.")
        if (feedback.length > 4000) return FormError("feedback", "O feedback pode ter até 4.000 caracteres.")
        if (notaInterna.length > 2000) return FormError("notaInterna", "A nota interna pode ter até 2.000 caracteres.")
        return null
    }

    fun toRequest() = AvaliacaoRequest(
        empresaId = empresaId,
        aplicacaoId = aplicacaoId,
        periodoInicio = periodoInicio!!,
        periodoFim = periodoFim!!,
        classificacao = classificacao!!,
        justificativa = justificativa.trim(),
        feedback = feedback.trim(),
        notaInterna = notaInterna.trim().ifEmpty { null },
    )

    companion object {
        fun from(a: AvaliacaoEquipeDto, empresaId: Long) = AvaliacaoForm(
            empresaId = empresaId,
            aplicacaoId = a.aplicacaoId,
            periodoInicio = a.periodoInicio,
            periodoFim = a.periodoFim,
            classificacao = a.classificacao,
            justificativa = a.justificativa.orEmpty(),
            feedback = a.feedback.orEmpty(),
            notaInterna = a.notaInterna.orEmpty(),
        )
    }
}
