package br.com.synergyone.android.data.termometro

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.AvaliacaoRequest
import br.com.synergyone.android.data.api.IdResponse
import kotlinx.serialization.encodeToString
import br.com.synergyone.android.data.api.TermometroClienteDto
import br.com.synergyone.android.data.api.TermometroEquipeDto

interface TermometroRepository {
    /** Só avaliações **publicadas** (o que o cliente vê no portal). Vale para qualquer papel. */
    suspend fun publicado(empresaId: Long?): ApiResult<TermometroClienteDto>

    /** Painel da equipe: resultados ao vivo, metas e avaliações (inclui rascunhos). Só equipe. */
    suspend fun painelEquipe(empresaId: Long?, desde: String?, ate: String?): ApiResult<TermometroEquipeDto>

    /** Cria (`id == null`) ou edita um **rascunho**. Avaliação publicada não se edita (409): corrige-se criando outra. */
    suspend fun salvarAvaliacao(id: Long?, request: AvaliacaoRequest): ApiResult<Long>

    /** Só `admin`/`gestao`. Congela os resultados do período; o cliente passa a ver. */
    suspend fun publicar(id: Long): ApiResult<Unit>
}

class ApiTermometroRepository(private val client: AppApiClient) : TermometroRepository {
    override suspend fun publicado(empresaId: Long?) =
        client.get<TermometroClienteDto>("/api/app/v1/termometro", params("empresaId" to empresaId))

    override suspend fun painelEquipe(empresaId: Long?, desde: String?, ate: String?) =
        client.get<TermometroEquipeDto>(
            "/api/app/v1/equipe/termometro",
            params("empresaId" to empresaId, "desde" to desde, "ate" to ate),
        )

    override suspend fun salvarAvaliacao(id: Long?, request: AvaliacaoRequest): ApiResult<Long> {
        val base = "/api/app/v1/equipe/termometro/avaliacoes"
        val raw = client.sendRaw(if (id == null) "POST" else "PUT", if (id == null) base else "$base/$id", AppApiClient.BODY.encodeToString(request))
        return client.decode(raw) { AppApiClient.JSON.decodeFromString<IdResponse>(it).id }
    }

    override suspend fun publicar(id: Long) =
        client.write("POST", "/api/app/v1/equipe/termometro/avaliacoes/$id/publicar", null)

    private fun params(vararg pairs: Pair<String, Any?>) =
        pairs.mapNotNull { (k, v) -> v?.let { k to it.toString() } }.toMap()
}
