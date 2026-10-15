package br.com.synergyone.android.data.lead

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.api.LeadsPage
import java.util.concurrent.ConcurrentHashMap

interface LeadRepository {
    /** Lead já carregado numa lista (o detalhe abre a partir dele; não há "GET de um lead"). */
    fun cached(id: Long): LeadDto?

    /** [cursor] é opaco: só repasse o `proximoCursor` recebido (nunca monte um). */
    suspend fun pagina(
        empresaId: Long? = null,
        etapa: String? = null,
        cursor: String? = null,
        desde: String? = null,
        limite: Int = 30,
    ): ApiResult<LeadsPage>
}

/** Teto de páginas (100 leads cada): 1.000 registros. Acima disso o app não calcula; o backend precisa agregar. */
const val MAX_PAGINAS = 10

/** Leads captados a partir de [desde] (`AAAA-MM-DD`, Brasília). [completo] = false quando o teto de páginas cortou a lista. */
data class LeadsDesde(val leads: List<LeadDto>, val total: Int, val completo: Boolean)

/**
 * Percorre as páginas de `GET /leads?desde=` (limite máximo de 100 por página) até [maxPaginas].
 * Nunca monta cursor: só repassa o `proximoCursor` recebido.
 */
suspend fun LeadRepository.captadosDesde(empresaId: Long?, desde: String?, etapa: String? = null, maxPaginas: Int = MAX_PAGINAS): ApiResult<LeadsDesde> {
    val todos = mutableListOf<LeadDto>()
    var cursor: String? = null
    var total = 0
    repeat(maxPaginas) {
        when (val r = pagina(empresaId = empresaId, etapa = etapa, cursor = cursor, desde = desde, limite = 100)) {
            is ApiResult.Error -> return r
            is ApiResult.Success -> {
                todos += r.data.leads
                total = r.data.total
                cursor = r.data.proximoCursor
                if (cursor == null) return ApiResult.Success(LeadsDesde(todos, total, completo = true))
            }
        }
    }
    return ApiResult.Success(LeadsDesde(todos, total, completo = false))
}

/** `GET /api/app/v1/leads`: leads captados com a etapa da jornada (sem notas internas). */
class ApiLeadRepository(private val client: AppApiClient) : LeadRepository {
    private val cache = ConcurrentHashMap<Long, LeadDto>()

    override fun cached(id: Long): LeadDto? = cache[id]

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
