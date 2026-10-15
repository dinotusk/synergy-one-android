package br.com.synergyone.android.data.api

import br.com.synergyone.android.data.remote.HttpResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Porta HTTP da API dos apps (`/api/app/v1`, ver docs do backend: API-APP.md). Autenticação só por
 * `Authorization: Bearer`; sem cookie e sem `apikey`. Falhas de rede viram `IOException`.
 */
interface AppApi {
    suspend fun request(
        method: String,
        path: String,
        token: String,
        query: Map<String, String> = emptyMap(),
        body: String? = null,
    ): HttpResult
}

class OkHttpAppApi(
    baseUrl: String,
    private val client: OkHttpClient = defaultClient(),
) : AppApi {
    private val base = baseUrl.trimEnd('/').toHttpUrl()

    override suspend fun request(
        method: String,
        path: String,
        token: String,
        query: Map<String, String>,
        body: String?,
    ): HttpResult {
        val url = base.newBuilder().apply {
            path.trim('/').split('/').forEach(::addPathSegment)
            query.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .method(method, body?.toRequestBody(JSON) ?: if (method == "GET") null else "".toRequestBody(JSON))
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use {
                HttpResult(it.code, it.body?.string().orEmpty(), it.header("x-request-id"))
            }
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        // Sem interceptor de logging: corpos autenticados nunca devem ir para logs.
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}
