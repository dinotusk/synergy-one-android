package br.com.synergyone.android.data.jornada

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.JornadaDetalheDto
import br.com.synergyone.android.data.api.JornadaEstadoRequest
import br.com.synergyone.android.data.api.JornadaLeadDto
import br.com.synergyone.android.data.api.JornadaPage
import br.com.synergyone.android.data.api.NotaRequest
import br.com.synergyone.android.data.api.RecebimentoRequest
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.encodeToString

/** Jornada do lead (só equipe): estado, histórico, notas e recebimentos. */
interface JornadaRepository {
    /**
     * Acha o estado de **um** lead. A API não tem "GET de um lead", então lê a listagem da empresa
     * no dia da captura (parâmetros publicados `empresaId`/`desde`/`ate`) e procura pelo `id`.
     * `null` no sucesso = o lead não está na listagem (ex.: lead escondido): o app mostra só leitura.
     */
    suspend fun estadoDoLead(leadId: Long, empresaId: Long, capturadoEm: String?): ApiResult<JornadaLocalizada?>
    suspend fun detalhe(leadId: Long): ApiResult<JornadaDetalheDto>
    suspend fun salvar(leadId: Long, estado: JornadaEstadoRequest): ApiResult<Unit>
    suspend fun adicionarNota(leadId: Long, texto: String): ApiResult<Unit>
    suspend fun registrarRecebimento(leadId: Long, valor: String, recebidoEm: String): ApiResult<Unit>
}

/** Estado do lead + listas de apoio (responsáveis, campanhas) e se a pessoa pode editar. */
data class JornadaLocalizada(val lead: JornadaLeadDto, val apoio: JornadaPage)

class ApiJornadaRepository(private val client: AppApiClient) : JornadaRepository {
    private val base = "/api/app/v1/equipe/jornada"

    override suspend fun estadoDoLead(leadId: Long, empresaId: Long, capturadoEm: String?): ApiResult<JornadaLocalizada?> {
        val dia = diaBrasilia(capturadoEm)
        var cursor: String? = null
        var primeira: JornadaPage? = null
        repeat(MAX_PAGINAS) {
            val query = buildMap {
                put("empresaId", empresaId.toString())
                put("limite", "100")
                dia?.let { put("desde", it); put("ate", it) }
                cursor?.let { put("cursor", it) }
            }
            when (val r = client.get<JornadaPage>(base, query)) {
                is ApiResult.Error -> return r
                is ApiResult.Success -> {
                    if (primeira == null) primeira = r.data
                    r.data.leads.firstOrNull { it.id == leadId }?.let { return ApiResult.Success(JornadaLocalizada(it, primeira!!)) }
                    cursor = r.data.proximoCursor ?: return ApiResult.Success(null)
                }
            }
        }
        return ApiResult.Success(null)
    }

    override suspend fun detalhe(leadId: Long) = client.get<JornadaDetalheDto>("$base/$leadId")

    override suspend fun salvar(leadId: Long, estado: JornadaEstadoRequest) =
        client.write("PUT", "$base/$leadId", AppApiClient.BODY.encodeToString(estado))

    override suspend fun adicionarNota(leadId: Long, texto: String) =
        client.write("POST", "$base/$leadId/notas", AppApiClient.BODY.encodeToString(NotaRequest(texto.trim())))

    override suspend fun registrarRecebimento(leadId: Long, valor: String, recebidoEm: String) =
        client.write("POST", "$base/$leadId/recebimentos", AppApiClient.BODY.encodeToString(RecebimentoRequest(valor, recebidoEm)))

    private companion object {
        const val MAX_PAGINAS = 5

        /** A API filtra por dia no horário de Brasília. */
        fun diaBrasilia(instante: String?): String? = runCatching {
            Instant.parse(instante!!).atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate().toString()
        }.getOrNull()
    }
}
