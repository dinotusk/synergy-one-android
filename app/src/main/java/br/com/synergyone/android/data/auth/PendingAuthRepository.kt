package br.com.synergyone.android.data.auth

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.model.AuthenticatedUser

/**
 * Implementação provisória usada enquanto o endpoint de login não é confirmado com o backend.
 *
 * Deliberadamente NÃO simula sucesso: qualquer tentativa de login retorna erro explicando que a
 * integração está pendente. Quando o contrato em docs/API_CONTRACTS.md for validado com o time de
 * backend, substituir esta classe por uma implementação real (ex.: via Retrofit) sem alterar a
 * interface [AuthRepository] nem os consumidores (ViewModel/UI).
 */
class PendingAuthRepository : AuthRepository {
    override suspend fun login(username: String, password: String): ApiResult<AuthenticatedUser> {
        return ApiResult.Error(
            message = "Integração com o backend ainda não configurada. Confirme o contrato em " +
                "docs/API_CONTRACTS.md com o time de backend antes de habilitar o login real.",
        )
    }
}
