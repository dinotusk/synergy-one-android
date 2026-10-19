package br.com.synergyone.android.data.biblioteca

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.BibliotecaPage

/** Áreas aceitas pelo servidor (`area`); qualquer outro valor seria `422`. */
val AREAS_BIBLIOTECA = listOf("Comercial", "Operação", "Criação", "Jurídico", "Inteligência", "Marca")

interface BibliotecaRepository {
    /** `GET /api/app/v1/biblioteca`. [cursor] é opaco: só repasse o `proximoCursor` recebido. */
    suspend fun pagina(empresaId: Long? = null, area: String? = null, cursor: String? = null, limite: Int = 30): ApiResult<BibliotecaPage>
}

class ApiBibliotecaRepository(private val client: AppApiClient) : BibliotecaRepository {
    override suspend fun pagina(empresaId: Long?, area: String?, cursor: String?, limite: Int) =
        client.get<BibliotecaPage>(
            "/api/app/v1/biblioteca",
            buildMap {
                put("limite", limite.coerceIn(1, 100).toString())
                empresaId?.let { put("empresaId", it.toString()) }
                area?.takeIf { it in AREAS_BIBLIOTECA }?.let { put("area", it) }
                cursor?.let { put("cursor", it) }
            },
        )
}

/** Só abre links HTTPS (nunca `http`, `file`, `intent`, `javascript`…). */
fun linkSeguro(url: String?): String? = url?.trim()?.takeIf { it.startsWith("https://", ignoreCase = true) && it.length > 8 && ' ' !in it }
