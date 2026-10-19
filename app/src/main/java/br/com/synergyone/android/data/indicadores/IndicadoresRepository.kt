package br.com.synergyone.android.data.indicadores

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.IndicadoresDto

/** Período pedido ao servidor: um `preset` (`30d`, `90d`, `mes`) que ele resolve, ou `de`/`ate` (`AAAA-MM-DD`). */
data class PeriodoIndicadores(val preset: String? = null, val de: String? = null, val ate: String? = null)

interface IndicadoresRepository {
    /** `GET /api/app/v1/indicadores`: números centrais da empresa, calculados no servidor (cliente e equipe). */
    suspend fun indicadores(empresaId: Long?, periodo: PeriodoIndicadores): ApiResult<IndicadoresDto>
}

class ApiIndicadoresRepository(private val client: AppApiClient) : IndicadoresRepository {
    override suspend fun indicadores(empresaId: Long?, periodo: PeriodoIndicadores) =
        client.get<IndicadoresDto>("/api/app/v1/indicadores", parametros(empresaId, periodo))
}

/** `preset` e `de`/`ate` não se misturam; sem nada, o servidor usa os últimos 30 dias. */
internal fun parametros(empresaId: Long?, p: PeriodoIndicadores): Map<String, String> = buildMap {
    empresaId?.let { put("empresaId", it.toString()) }
    if (p.preset != null) put("preset", p.preset)
    else if (p.de != null && p.ate != null) { put("de", p.de); put("ate", p.ate) }
}
