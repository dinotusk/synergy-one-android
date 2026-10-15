package br.com.synergyone.android.data.freshness

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApi
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.auth.AuthRepository
import br.com.synergyone.android.data.lead.ApiLeadRepository
import br.com.synergyone.android.data.remote.HttpResult
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataFreshnessTest {
    private var agora = 1_000_000L
    private val f = DataFreshness(now = { agora }, ttlMillis = 5 * 60_000)

    @Test fun `dado recem lido nao esta velho`() {
        assertFalse(f.isStale(loadedVersion = 0, loadedAtMillis = agora))
    }

    @Test fun `passado o tempo o dado fica velho`() {
        val lidoEm = agora
        agora += 5 * 60_000 + 1
        assertTrue(f.isStale(0, lidoEm))
    }

    @Test fun `uma gravacao invalida tudo o que foi lido antes`() {
        assertFalse(f.isStale(0, agora))
        f.invalidate()
        assertTrue(f.isStale(0, agora))
        assertFalse(f.isStale(f.version.value, agora)) // quem leu depois da gravacao esta em dia
    }

    private fun client(onMutation: () -> Unit, code: Int = 200) = AppApiClient(
        api = object : AppApi {
            override suspend fun request(method: String, path: String, token: String, query: Map<String, String>, body: String?) = HttpResult(code, "{}", null)
        },
        auth = authQueSempreTemToken(),
        onMutation = onMutation,
    )

    private fun authQueSempreTemToken(): AuthRepository = object : AuthRepository {
        override val currentUser: StateFlow<AuthenticatedUser?> = MutableStateFlow(null)
        override suspend fun login(email: String, password: String): ApiResult<AuthenticatedUser> = error("n/a")
        override suspend fun restoreSession(): AuthenticatedUser? = null
        override suspend fun logout() = Unit
        override suspend fun accessToken(rejected: String?): String? = "t"
    }

    @Test fun `gravacao bem sucedida avisa, leitura e falha nao`() = runBlocking {
        var avisos = 0
        client({ avisos++ }).apply {
            getRaw("/api/app/v1/leads")
            assertEquals(0, avisos)
            write("PUT", "/api/app/v1/equipe/jornada/1", "{}")
            assertEquals(1, avisos)
        }
        var falha = 0
        client({ falha++ }, code = 422).write("PUT", "/x", "{}")
        assertEquals(0, falha)
    }

    @Test fun `descartar a sessao limpa o que ficou em memoria`() {
        val repo = ApiLeadRepository(client({}))
        repo.clear()
        assertNull(repo.cached(1))
    }
}
