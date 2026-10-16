package br.com.synergyone.android.data.jornada

import br.com.synergyone.android.data.api.JornadaEstadoRequest
import br.com.synergyone.android.data.api.JornadaLeadDto
import java.math.BigDecimal
import java.math.RoundingMode

/** Erro de validação local, no mesmo formato do 422 do servidor (`campo` + mensagem). */
data class FormError(val campo: String, val message: String)

/**
 * Converte o que a pessoa digitou em valor decimal canônico (`"1500.00"`), o formato que a API
 * espera na **entrada**. Aceita `1500`, `1500.5`, `1.500,00` e `R$ 1.500,50`. `null` = inválido.
 */
fun parseMoneyInput(raw: String): String? {
    val limpo = raw.trim().removePrefix("R$").trim().replace(" ", "")
    if (limpo.isEmpty()) return null
    val normal = when {
        limpo.contains(',') -> limpo.replace(".", "").replace(',', '.')   // 1.500,50 -> 1500.50
        limpo.count { it == '.' } > 1 -> limpo.replace(".", "")            // 1.500.000 -> 1500000
        else -> limpo
    }
    val valor = normal.toBigDecimalOrNull() ?: return null
    if (valor.signum() < 0) return null
    return valor.setScale(2, RoundingMode.HALF_UP).toPlainString()
}

fun centavosToInput(centavos: Long?): String =
    centavos?.let { BigDecimal(it).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY).toPlainString() }.orEmpty()

/**
 * Estado editável da jornada. As regras de [validate] espelham o servidor (API-APP.md, `PUT`):
 * `ganho` exige venda > 0 e data; `perdido` exige motivo (≤300) e data; `clienteEmpresaId` só com
 * `ganho`. O servidor continua sendo a autoridade: isto só evita ida e volta.
 */
data class JornadaForm(
    val etapa: String,
    val responsavelId: String? = null,
    val campanhaId: Long? = null,
    val propostaValor: String = "",
    val propostaEnviadaEm: String? = null,
    val vendaValor: String = "",
    val vendaFechadaEm: String? = null,
    val motivoPerda: String = "",
    val perdidoEm: String? = null,
    val clienteEmpresaId: Long? = null,
) {
    fun validate(): FormError? {
        if (propostaValor.isNotBlank() && parseMoneyInput(propostaValor) == null) return FormError("propostaValor", "Valor da proposta inválido.")
        if (etapa == "ganho") {
            val v = parseMoneyInput(vendaValor)
            if (v == null || v.toBigDecimal().signum() <= 0) return FormError("vendaValor", "Informe o valor da venda.")
            if (vendaFechadaEm.isNullOrBlank()) return FormError("vendaFechadaEm", "Informe a data do fechamento.")
        }
        if (etapa == "perdido") {
            if (motivoPerda.isBlank()) return FormError("motivoPerda", "Informe o motivo da perda.")
            if (motivoPerda.trim().length > 300) return FormError("motivoPerda", "O motivo pode ter até 300 caracteres.")
            if (perdidoEm.isNullOrBlank()) return FormError("perdidoEm", "Informe a data da perda.")
        }
        return null
    }

    /** Estado completo para o `PUT`: o que não pertence à etapa escolhida vai como `null`. */
    fun toRequest(): JornadaEstadoRequest = JornadaEstadoRequest(
        etapa = etapa,
        responsavelId = responsavelId,
        campanhaId = campanhaId,
        propostaValor = parseMoneyInput(propostaValor),
        propostaEnviadaEm = propostaEnviadaEm?.takeIf { it.isNotBlank() },
        vendaValor = if (etapa == "ganho") parseMoneyInput(vendaValor) else null,
        vendaFechadaEm = if (etapa == "ganho") vendaFechadaEm else null,
        motivoPerda = if (etapa == "perdido") motivoPerda.trim() else null,
        perdidoEm = if (etapa == "perdido") perdidoEm else null,
        clienteEmpresaId = if (etapa == "ganho") clienteEmpresaId else null,
    )

    companion object {
        fun from(lead: JornadaLeadDto) = JornadaForm(
            etapa = lead.etapa,
            responsavelId = lead.responsavelId,
            campanhaId = lead.campanhaId,
            propostaValor = centavosToInput(lead.propostaCentavos),
            propostaEnviadaEm = lead.propostaEnviadaEm,
            vendaValor = centavosToInput(lead.vendaCentavos),
            vendaFechadaEm = lead.vendaFechadaEm,
            motivoPerda = lead.motivoPerda.orEmpty(),
            perdidoEm = lead.perdidoEm,
            clienteEmpresaId = lead.clienteEmpresaId,
        )
    }
}
