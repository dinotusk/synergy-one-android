package br.com.synergyone.android.core.result

/** Resultado genérico de uma operação de rede/repositório, usado enquanto não há integração real com o backend. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>
}
