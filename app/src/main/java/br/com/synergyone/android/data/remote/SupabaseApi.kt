package br.com.synergyone.android.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Resposta HTTP crua. Falhas de rede/timeout viram [IOException]. */
data class HttpResult(val code: Int, val body: String, val requestId: String? = null) {
    val isSuccess: Boolean get() = code in 200..299

    /** PostgREST recusou o JWT (expirado/inválido): vale uma renovação de token. */
    val isJwtRejected: Boolean get() = code == 401 || "PGRST301" in body
}

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: AuthUserDto,
)

@Serializable
data class AuthUserDto(val id: String, val email: String? = null)

/** Porta HTTP do Supabase Auth (login, renovação e logout). Os dados vêm da API dos apps. */
interface SupabaseApi {
    suspend fun passwordGrant(email: String, password: String): HttpResult
    suspend fun refreshGrant(refreshToken: String): HttpResult
    suspend fun logout(accessToken: String): HttpResult
}

class OkHttpSupabaseApi(
    baseUrl: String,
    private val anonKey: String,
    private val client: OkHttpClient = defaultClient(),
) : SupabaseApi {
    private val base = baseUrl.trimEnd('/').toHttpUrl()

    override suspend fun passwordGrant(email: String, password: String) = post(
        path = listOf("auth", "v1", "token"),
        query = mapOf("grant_type" to "password"),
        body = Json.encodeToString(mapOf("email" to email, "password" to password)),
    )

    override suspend fun refreshGrant(refreshToken: String) = post(
        path = listOf("auth", "v1", "token"),
        query = mapOf("grant_type" to "refresh_token"),
        body = Json.encodeToString(mapOf("refresh_token" to refreshToken)),
    )

    override suspend fun logout(accessToken: String) = post(
        path = listOf("auth", "v1", "logout"),
        query = mapOf("scope" to "local"),
        body = "",
        accessToken = accessToken,
    )

    private suspend fun post(
        path: List<String>,
        query: Map<String, String>,
        body: String,
        accessToken: String? = null,
    ) = execute(
        Request.Builder().url(url(path, query))
            .post(body.toRequestBody(JSON))
            .authHeaders(accessToken)
            .build(),
    )

    private fun url(path: List<String>, query: Map<String, String>) = base.newBuilder().apply {
        path.forEach(::addPathSegment)
        query.forEach { (k, v) -> addQueryParameter(k, v) }
    }.build()

    private fun Request.Builder.authHeaders(accessToken: String?) = apply {
        header("apikey", anonKey)
        header("Accept", "application/json")
        if (accessToken != null) header("Authorization", "Bearer $accessToken")
    }

    private suspend fun execute(request: Request): HttpResult = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { HttpResult(it.code, it.body?.string().orEmpty()) }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        // Sem interceptor de logging: corpos de auth nunca devem ir para logs.
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
