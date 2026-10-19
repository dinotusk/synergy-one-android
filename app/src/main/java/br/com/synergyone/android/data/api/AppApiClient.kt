package br.com.synergyone.android.data.api

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.AuthRepository
import br.com.synergyone.android.data.remote.HttpResult
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Códigos de erro do contrato (`error`) que a UI trata de forma especial. */
object ApiErrors {
    const val SESSION_INVALID = "sessao_invalida"
    const val NO_ACCESS = "sem_acesso"
    const val NO_PERMISSION = "sem_permissao"
    const val UNAVAILABLE = "indisponivel"
    const val NETWORK = "rede"
}

@Serializable
private data class ErrorBody(val error: String? = null, val message: String? = null, val motivo: String? = null, val campo: String? = null)

/**
 * Chamador autenticado da API dos apps: usa o token da sessão, renova **uma vez** em 401 e
 * traduz os erros do contrato em mensagens. Falha de infraestrutura (503/rede) nunca desloga.
 */
class AppApiClient(
    private val api: AppApi,
    private val auth: AuthRepository,
    /** Chamado após qualquer gravação bem-sucedida (não GET): quem leu antes passa a ter dado velho. */
    private val onMutation: () -> Unit = {},
) {
    suspend fun getRaw(path: String, query: Map<String, String> = emptyMap()): ApiResult<String> =
        call("GET", path, query, null)

    suspend fun sendRaw(method: String, path: String, body: String?): ApiResult<String> =
        call(method, path, emptyMap(), body)

    /** Escrita sem corpo de resposta útil (204/200/201): devolve sucesso ou o erro do contrato. */
    suspend fun write(method: String, path: String, body: String?): ApiResult<Unit> =
        when (val r = sendRaw(method, path, body)) {
            is ApiResult.Error -> r
            is ApiResult.Success -> ApiResult.Success(Unit)
        }

    suspend inline fun <reified T> get(path: String, query: Map<String, String> = emptyMap()): ApiResult<T> =
        decode(getRaw(path, query)) { JSON.decodeFromString<T>(it) }

    fun <T> decode(raw: ApiResult<String>, parse: (String) -> T): ApiResult<T> = when (raw) {
        is ApiResult.Error -> raw
        is ApiResult.Success -> try {
            ApiResult.Success(parse(raw.data))
        } catch (e: Exception) {
            ApiResult.Error(MSG_UNEXPECTED, e)
        }
    }

    private suspend fun call(method: String, path: String, query: Map<String, String>, body: String?): ApiResult<String> {
        val token = auth.accessToken() ?: return sessionExpired()
        return try {
            var response = api.request(method, path, token, query, body)
            if (response.code == 401) {
                // Uma única renovação (serializada no repositório); depois refaz a chamada original.
                val renewed = auth.accessToken(rejected = token) ?: return sessionExpired()
                response = api.request(method, path, renewed, query, body)
            }
            toResult(response).also { if (it is ApiResult.Success && method != "GET") onMutation() }
        } catch (e: IOException) {
            ApiResult.Error(MSG_NETWORK, e, ApiErrors.NETWORK)
        }
    }

    private fun sessionExpired() = ApiResult.Error(MSG_SESSION, code = ApiErrors.SESSION_INVALID)

    private fun toResult(r: HttpResult): ApiResult<String> {
        if (r.isSuccess) return ApiResult.Success(r.body)
        val parsed = try { JSON.decodeFromString<ErrorBody>(r.body) } catch (_: Exception) { null }
        val code = parsed?.error
        val message = when {
            r.code == 401 -> MSG_SESSION
            r.code == 403 && code == ApiErrors.NO_ACCESS ->
                if (parsed.motivo == "inativo") MSG_SUSPENDED else MSG_NO_ACCESS
            r.code == 429 -> MSG_RATE_LIMIT
            r.code >= 500 -> MSG_UNAVAILABLE
            // 403 sem_permissao, 404, 409, 422: o servidor já manda a frase pronta em português.
            else -> parsed?.message?.takeIf { it.isNotBlank() } ?: MSG_UNEXPECTED
        }
        val finalCode = if (r.code == 401) ApiErrors.SESSION_INVALID else code
        return ApiResult.Error(message, code = finalCode, field = parsed?.campo)
    }

    companion object {
        val JSON = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        /** Corpos de gravação: `null` vai explícito (a jornada é "estado completo"). */
        val BODY = Json { explicitNulls = true; encodeDefaults = true }
        const val MSG_SESSION = "Sua sessão expirou. Entre novamente."
        const val MSG_NO_ACCESS = "Esta conta não tem acesso ao Synergy One."
        const val MSG_SUSPENDED = "Este acesso está suspenso. Fale com a Synergy."
        const val MSG_RATE_LIMIT = "Muitas requisições. Aguarde um instante e tente de novo."
        const val MSG_UNAVAILABLE = "Serviço temporariamente indisponível. Tente novamente em instantes."
        const val MSG_NETWORK = "Não foi possível conectar. Verifique sua internet e tente novamente."
        const val MSG_UNEXPECTED = "Não foi possível carregar os dados. Tente novamente."
    }
}
