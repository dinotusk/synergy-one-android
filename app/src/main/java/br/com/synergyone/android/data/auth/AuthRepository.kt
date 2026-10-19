package br.com.synergyone.android.data.auth

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import kotlinx.coroutines.flow.StateFlow

/** Porta de autenticação. Contrato real em docs/API_CONTRACTS.md (Supabase Auth + PostgREST/RLS). */
interface AuthRepository {
    /** Usuário da sessão atual (`null` = deslogado). Atualizado em login, restauração e logout. */
    val currentUser: StateFlow<AuthenticatedUser?>

    /** Login completo: credenciais no Supabase Auth + checagem de acesso ao Synergy One. */
    suspend fun login(email: String, password: String): ApiResult<AuthenticatedUser>

    /**
     * Restaura a sessão salva (renovando e revalidando o acesso). `null` = não há sessão utilizável
     * e o app deve mostrar o login. Falha de rede NÃO apaga a sessão salva.
     */
    suspend fun restoreSession(): AuthenticatedUser?

    /** Encerra a sessão local (e tenta revogar no servidor). Sempre apaga os dados locais. */
    suspend fun logout()

    /**
     * Access token válido para chamadas ao PostgREST, renovando se faltar pouco para expirar.
     * Passe [rejected] ao receber 401/PGRST301 para forçar uma única renovação (serializada).
     * `null` = sem sessão.
     */
    suspend fun accessToken(rejected: String? = null): String?
}
