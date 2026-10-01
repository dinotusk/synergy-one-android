package br.com.synergyone.android.data.auth

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.model.AuthenticatedUser

/** Porta de autenticação. A implementação real depende do contrato de API ainda a ser confirmado com o backend. */
interface AuthRepository {
    suspend fun login(username: String, password: String): ApiResult<AuthenticatedUser>
}
