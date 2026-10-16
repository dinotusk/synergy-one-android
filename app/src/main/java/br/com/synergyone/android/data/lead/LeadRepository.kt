package br.com.synergyone.android.data.lead

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.api.LeadsPage
import java.util.concurrent.ConcurrentHashMap

interface LeadRepository {
    /** Lead já carregado numa lista (o detalhe abre a partir dele; não há "GET de um lead"). */
    fun cached(id: Long): LeadDto?

    /** Descarta o que ficou em memória da sessão anterior (logout/troca de conta). */
    fun clear() {}

    /** [cursor] é opaco: só repasse o `proximoCursor` recebido (nunca monte um). */
    suspend fun pagina(
        empresaId: Long? = null,
        etapa: String? = null,
        cursor: String? = null,
        desde: String? = null,
        limite: Int = 30,
    ): ApiResult<LeadsPage>
}

/** `GET /api/app/v1/leads`: leads captados com a etapa da jornada (sem notas internas). */
class ApiLeadRepository(private val client: AppApiClient) : LeadRepository {
    private val cache = ConcurrentHashMap<Long, LeadDto>()

    override fun cached(id: Long): LeadDto? = cache[id]

    override fun clear() = cache.clear()

    override suspend fun pagina(empresaId: Long?, etapa: String?, cursor: String?, desde: String?, limite: Int) =
        client.get<LeadsPage>(
            "/api/app/v1/leads",
            buildMap {
                put("limite", limite.coerceIn(1, 100).toString())
                desde?.let { put("desde", it) }
                empresaId?.let { put("empresaId", it.toString()) }
                // "captado" não é filtrável na API.
                etapa?.takeIf { it != "captado" }?.let { put("etapa", it) }
                cursor?.let { put("cursor", it) }
            },
        ).also { r -> if (r is ApiResult.Success) r.data.leads.forEach { cache[it.id] = it } }
}
