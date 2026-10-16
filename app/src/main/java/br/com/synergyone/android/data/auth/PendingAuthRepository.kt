package br.com.synergyone.android.data.auth

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Usado quando `supabase.url` / `supabase.anonKey` não estão configurados neste build.
 * Deliberadamente NÃO simula sucesso.
 */
class PendingAuthRepository : AuthRepository {
    override val currentUser: StateFlow<AuthenticatedUser?> = MutableStateFlow(null)

    override suspend fun login(email: String, password: String): ApiResult<AuthenticatedUser> =
        ApiResult.Error(
            message = "Backend não configurado neste build. Defina supabase.url e supabase.anonKey " +
                "em local.properties (ver README).",
        )

    override suspend fun restoreSession(): AuthenticatedUser? = null
    override suspend fun logout() = Unit
    override suspend fun accessToken(rejected: String?): String? = null
}
