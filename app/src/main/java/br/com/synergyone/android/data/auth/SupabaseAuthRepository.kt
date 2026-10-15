package br.com.synergyone.android.data.auth

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.Papel
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.data.remote.HttpResult
import br.com.synergyone.android.data.remote.SupabaseApi
import br.com.synergyone.android.data.remote.TokenResponse
import br.com.synergyone.android.data.api.AppApi
import br.com.synergyone.android.data.api.MeDto
import br.com.synergyone.android.data.auth.model.EmpresaRef
import br.com.synergyone.android.data.session.Session
import br.com.synergyone.android.data.session.SessionStore
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Login/renovação/logout no Supabase Auth; autorização via `GET /api/app/v1/me` (contrato do
 * backend: MOBILE-AUTH.md). O app nunca decide acesso sozinho.
 */
class SupabaseAuthRepository(
    private val api: SupabaseApi,
    private val appApi: AppApi,
    private val store: SessionStore,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthenticatedUser?>(null)
    override val currentUser: StateFlow<AuthenticatedUser?> = _currentUser.asStateFlow()

    // Serializa renovações: dois refresh paralelos com o mesmo token podem invalidar a sessão.
    private val sessionLock = Mutex()

    override suspend fun login(email: String, password: String): ApiResult<AuthenticatedUser> {
        val normalized = email.trim().lowercase()
        if (normalized.length > MAX_EMAIL || !EMAIL_REGEX.matches(normalized)) {
            return ApiResult.Error("Informe um e-mail válido.")
        }
        if (password.isEmpty() || password.length > MAX_PASSWORD) {
            return ApiResult.Error(MSG_BAD_CREDENTIALS)
        }

        val response = try {
            api.passwordGrant(normalized, password)
        } catch (e: IOException) {
            return ApiResult.Error(MSG_NETWORK, e)
        }
        if (!response.isSuccess) return ApiResult.Error(loginErrorMessage(response))
        val tokens = parseTokens(response) ?: return ApiResult.Error(MSG_UNEXPECTED)

        val session = tokens.toSession()
        return when (val access = loadAccess(session.accessToken, session.userId)) {
            is Access.Granted -> {
                sessionLock.withLock { store.save(session) }
                _currentUser.value = access.user
                ApiResult.Success(access.user)
            }
            is Access.Denied -> {
                revokeQuietly(session.accessToken)
                ApiResult.Error(access.message)
            }
            Access.Unauthorized, is Access.Failure -> {
                revokeQuietly(session.accessToken)
                ApiResult.Error((access as? Access.Failure)?.message ?: MSG_UNEXPECTED)
            }
        }
    }

    override suspend fun restoreSession(): AuthenticatedUser? {
        val session = sessionLock.withLock { store.load() } ?: return null
        var token = accessToken() ?: return null
        var access = loadAccess(token, session.userId)
        if (access == Access.Unauthorized) {
            token = accessToken(rejected = token) ?: return null
            access = loadAccess(token, session.userId)
        }
        return when (access) {
            is Access.Granted -> access.user.also { _currentUser.value = it }
            is Access.Denied, Access.Unauthorized -> {
                logout()
                null
            }
            is Access.Failure -> null // provável falha de rede: mantém a sessão salva
        }
    }

    override suspend fun logout() {
        _currentUser.value = null
        val session = sessionLock.withLock { store.load().also { store.clear() } }
        if (session != null) revokeQuietly(session.accessToken)
    }

    override suspend fun accessToken(rejected: String?): String? = sessionLock.withLock {
        val session = store.load() ?: return@withLock null
        val stillValid = session.expiresAt - REFRESH_MARGIN_SECONDS > nowSeconds()
        // Se outra chamada já renovou, o token guardado difere do rejeitado: basta usá-lo.
        val forced = rejected != null && rejected == session.accessToken
        if (stillValid && !forced) return@withLock session.accessToken
        refreshLocked(session)?.accessToken
    }

    /** Chamar com [sessionLock] já adquirido. */
    private suspend fun refreshLocked(session: Session): Session? {
        val response = try {
            api.refreshGrant(session.refreshToken)
        } catch (_: IOException) {
            return null
        }
        if (response.isSuccess) {
            val renewed = parseTokens(response)?.toSession() ?: return null
            store.save(renewed) // sempre substitui o refresh token pelo novo
            return renewed
        }
        // refresh_token_not_found / already_used / session_not_found etc.: sessão encerrada.
        // 429 e 5xx são transitórios: mantém a sessão.
        if (response.code in 400..499 && response.code != 429) store.clear()
        return null
    }

    private sealed interface Access {
        data class Granted(val user: AuthenticatedUser) : Access
        data class Denied(val message: String) : Access
        data object Unauthorized : Access
        data class Failure(val message: String) : Access
    }

    /** `GET /me` (403 = sem acesso; 401 = token recusado; 503 = infraestrutura, nunca vira 403). */
    private suspend fun loadAccess(token: String, userId: String): Access {
        val response = try {
            appApi.request("GET", ME_PATH, token)
        } catch (_: IOException) {
            return Access.Failure(MSG_NETWORK)
        }
        return when {
            response.code == 401 -> Access.Unauthorized
            response.code == 403 -> Access.Denied(
                if ("inativo" in response.body) MSG_SUSPENDED else MSG_NO_ACCESS,
            )
            !response.isSuccess -> Access.Failure(if (response.code >= 500) MSG_UNAVAILABLE else MSG_UNEXPECTED)
            else -> {
                val me = decode<MeDto>(response.body) ?: return Access.Failure(MSG_UNEXPECTED)
                toUser(me, userId)
            }
        }
    }

    private fun toUser(me: MeDto, fallbackId: String): Access {
        val role = when (me.tipo) {
            "equipe" -> UserRole.TEAM
            "cliente" -> UserRole.CLIENT
            else -> return Access.Denied(MSG_NO_ACCESS) // valor desconhecido = sem acesso
        }
        val papel = Papel.fromWire(me.papel) ?: return Access.Denied(MSG_NO_ACCESS)
        // Restrição do banco: tipo=cliente <=> papel=cliente. Divergência = sem acesso.
        if ((role == UserRole.CLIENT) != (papel == Papel.CLIENTE)) return Access.Denied(MSG_NO_ACCESS)
        return Access.Granted(
            AuthenticatedUser(
                id = me.id.ifBlank { fallbackId },
                displayName = me.nome?.takeIf { it.isNotBlank() } ?: me.email.orEmpty(),
                email = me.email.orEmpty(),
                role = role,
                papel = papel,
                empresas = me.empresas.map { EmpresaRef(it.id, it.nome) },
                todasEmpresas = me.todasEmpresas,
                semEmpresa = me.semEmpresa,
            ),
        )
    }

    private suspend fun revokeQuietly(accessToken: String) {
        try {
            api.logout(accessToken)
        } catch (_: IOException) {
            // O logout local já aconteceu; a revogação remota é best-effort.
        }
    }

    private fun loginErrorMessage(r: HttpResult): String = when (r.code) {
        429 -> MSG_RATE_LIMIT
        400, 401, 422 -> MSG_BAD_CREDENTIALS // a web não revela se o e-mail existe
        else -> MSG_NETWORK
    }

    private fun parseTokens(r: HttpResult): TokenResponse? = decode(r.body)

    private fun TokenResponse.toSession() = Session(
        userId = user.id,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAt = expiresAt ?: (nowSeconds() + expiresIn),
    )

    private inline fun <reified T> decode(body: String): T? =
        try { JSON.decodeFromString<T>(body) } catch (_: Exception) { null }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        const val MAX_EMAIL = 254
        const val MAX_PASSWORD = 200
        const val REFRESH_MARGIN_SECONDS = 60L

        const val MSG_BAD_CREDENTIALS = "E-mail ou senha incorretos."
        const val ME_PATH = "/api/app/v1/me"
        const val MSG_SUSPENDED = "Este acesso está suspenso. Fale com a Synergy."
        const val MSG_UNAVAILABLE = "Serviço temporariamente indisponível. Tente novamente em instantes."
        const val MSG_NO_ACCESS = "Esta conta não tem acesso ao Synergy One."
        const val MSG_RATE_LIMIT = "Muitas tentativas. Tente novamente em instantes."
        const val MSG_NETWORK = "Não foi possível conectar. Verifique sua internet e tente novamente."
        const val MSG_UNEXPECTED = "Resposta inesperada do servidor. Tente novamente."
    }
}
