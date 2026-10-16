package br.com.synergyone.android.data.auth

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.model.Papel
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.data.api.AppApi
import br.com.synergyone.android.data.remote.HttpResult
import br.com.synergyone.android.data.remote.SupabaseApi
import br.com.synergyone.android.data.session.InMemorySessionStore
import br.com.synergyone.android.data.session.Session
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun tokens(access: String = "acc1", refresh: String = "ref1", expiresAt: Long = 10_000) =
    HttpResult(
        200,
        """{"access_token":"$access","refresh_token":"$refresh","expires_in":3600,"expires_at":$expiresAt,
           "token_type":"bearer","user":{"id":"u1","email":"maria@x.com"}}""",
    )

private fun me(tipo: String = "equipe", papel: String = "gestao", semEmpresa: Boolean = false) = HttpResult(
    200,
    """{"id":"u1","nome":"Maria","email":"maria@x.com","tipo":"$tipo","papel":"$papel",
       "todasEmpresas":${tipo == "equipe"},
       "empresas":${if (tipo == "cliente") """[{"id":3,"nome":"ForLife"},{"id":7,"nome":"Acme"}]""" else "[]"},
       "semEmpresa":$semEmpresa,"sessaoExpiraEm":"2026-10-01T15:30:00.000Z"}""",
)

private fun semAcesso(motivo: String) = HttpResult(403, """{"error":"sem_acesso","motivo":"$motivo","message":"x"}""")

private class FakeApi : SupabaseApi {
    var login = tokens()
    var refresh = tokens("acc2", "ref2", 20_000)
    var networkDown = false
    var refreshCalls = 0
    var logoutCalls = 0
    var lastLoginEmail: String? = null

    private fun net() { if (networkDown) throw IOException("offline") }

    override suspend fun passwordGrant(email: String, password: String): HttpResult { net(); lastLoginEmail = email; return login }
    override suspend fun refreshGrant(refreshToken: String): HttpResult { net(); refreshCalls++; return refresh }
    override suspend fun logout(accessToken: String): HttpResult { net(); logoutCalls++; return HttpResult(204, "") }
}

/** Simula `GET /api/app/v1/me`. */
private class FakeAppApi(private val supabase: FakeApi) : AppApi {
    var me = me()
    /** Tokens que o servidor considera expirados. */
    val expiredTokens = mutableSetOf<String>()
    var paths = mutableListOf<String>()

    override suspend fun request(method: String, path: String, token: String, query: Map<String, String>, body: String?): HttpResult {
        if (supabase.networkDown) throw IOException("offline")
        paths += path
        return if (token in expiredTokens) HttpResult(401, """{"error":"sessao_invalida"}""") else me
    }
}

class SupabaseAuthRepositoryTest {
    private val api = FakeApi()
    private val app = FakeAppApi(api)
    private val store = InMemorySessionStore()
    private var now = 1_000L
    private val repo = SupabaseAuthRepository(api, app, store) { now }

    private fun error(r: ApiResult<*>) = (r as ApiResult.Error).message

    @Test
    fun `login de equipe normaliza o e-mail, salva a sessao e deriva papel`() = runTest {
        val result = repo.login("  Maria@X.com ", "senha")

        val user = (result as ApiResult.Success).data
        assertEquals("maria@x.com", api.lastLoginEmail)
        assertEquals(UserRole.TEAM, user.role)
        assertEquals(Papel.GESTAO, user.papel)
        assertTrue(user.canWrite && user.isAdminOrManager && !user.isAdmin)
        assertEquals("acc1", store.load()?.accessToken)
    }

    @Test
    fun `login de cliente carrega empresas`() = runTest {
        app.me = me("cliente", "cliente")
        val user = (repo.login("maria@x.com", "s") as ApiResult.Success).data
        assertEquals(UserRole.CLIENT, user.role)
        assertEquals(listOf(3L, 7L), user.empresas.map { it.id })
        assertEquals("ForLife", user.empresas.first().nome)
        assertTrue(!user.todasEmpresas)
        assertTrue(!user.canWrite)
    }

    @Test
    fun `credenciais invalidas viram mensagem unica e nada e salvo`() = runTest {
        for (code in listOf(400, 401, 422)) {
            api.login = HttpResult(code, """{"error_code":"invalid_credentials"}""")
            assertEquals("E-mail ou senha incorretos.", error(repo.login("maria@x.com", "x")))
        }
        assertNull(store.load())
    }

    @Test
    fun `429 e falha de rede tem mensagens proprias`() = runTest {
        api.login = HttpResult(429, "")
        assertTrue(error(repo.login("maria@x.com", "x")).startsWith("Muitas tentativas"))
        api.networkDown = true
        assertTrue(error(repo.login("maria@x.com", "x")).contains("conectar"))
    }

    @Test
    fun `email invalido nem chega ao servidor`() = runTest {
        assertEquals("Informe um e-mail válido.", error(repo.login("maria", "x")))
        assertNull(api.lastLoginEmail)
    }

    @Test
    fun `403 do me nega acesso, revoga a sessao e diferencia suspensa de sem cadastro`() = runTest {
        app.me = semAcesso("sem_cadastro")
        assertEquals("Esta conta não tem acesso ao Synergy One.", error(repo.login("maria@x.com", "x")))
        app.me = semAcesso("inativo")
        assertEquals("Este acesso está suspenso. Fale com a Synergy.", error(repo.login("maria@x.com", "x")))
        assertEquals(2, api.logoutCalls)
        assertNull(store.load())
    }

    @Test
    fun `tipo ou papel desconhecido ou inconsistente tambem nega acesso`() = runTest {
        val cases = listOf(
            me(tipo = "outro", papel = "admin"),
            me(papel = "cliente"),                 // equipe nunca tem papel cliente
            me(tipo = "cliente", papel = "admin"), // cliente nunca tem papel de equipe
            me(papel = "desconhecido"),
        )
        for (case in cases) {
            app.me = case
            assertEquals("Esta conta não tem acesso ao Synergy One.", error(repo.login("maria@x.com", "x")))
            assertNull(store.load())
        }
    }

    @Test
    fun `503 do me nao nega acesso nem apaga sessao salva`() = runTest {
        store.save(Session("u1", "acc1", "ref1", expiresAt = now + 3600))
        app.me = HttpResult(503, """{"error":"indisponivel"}""")

        assertNull(repo.restoreSession())
        assertNotNull(store.load())
        assertEquals(0, api.logoutCalls)
        assertTrue(error(repo.login("maria@x.com", "x")).contains("indispon"))
    }

    @Test
    fun `cliente sem empresa entra com semEmpresa`() = runTest {
        app.me = me("cliente", "cliente", semEmpresa = true)
        assertTrue((repo.login("maria@x.com", "x") as ApiResult.Success).data.semEmpresa)
    }

    @Test
    fun `me e chamado no caminho oficial`() = runTest {
        repo.login("maria@x.com", "x")
        assertEquals(listOf("/api/app/v1/me"), app.paths)
    }

    @Test
    fun `restoreSession sem sessao devolve null`() = runTest {
        assertNull(repo.restoreSession())
    }

    @Test
    fun `restoreSession renova token expirado e guarda o novo refresh token`() = runTest {
        store.save(Session("u1", "old", "refOld", expiresAt = now + 10)) // dentro da margem de 60s
        val user = repo.restoreSession()

        assertNotNull(user)
        assertEquals(1, api.refreshCalls)
        assertEquals("acc2", store.load()?.accessToken)
        assertEquals("ref2", store.load()?.refreshToken)
    }

    @Test
    fun `restoreSession tenta renovar uma vez ao receber PGRST301`() = runTest {
        store.save(Session("u1", "stale", "ref1", expiresAt = now + 3600))
        app.expiredTokens += "stale"

        assertNotNull(repo.restoreSession())
        assertEquals(1, api.refreshCalls)
        assertEquals("acc2", store.load()?.accessToken)
    }

    @Test
    fun `usuario desativado encerra a sessao ao restaurar`() = runTest {
        store.save(Session("u1", "acc1", "ref1", expiresAt = now + 3600))
        app.me = semAcesso("inativo")

        assertNull(repo.restoreSession())
        assertNull(store.load())
        assertEquals(1, api.logoutCalls)
    }

    @Test
    fun `refresh rejeitado pelo servidor limpa a sessao`() = runTest {
        store.save(Session("u1", "old", "refGone", expiresAt = now - 5))
        api.refresh = HttpResult(400, """{"error_code":"refresh_token_already_used"}""")

        assertNull(repo.restoreSession())
        assertNull(store.load())
    }

    @Test
    fun `sem internet a sessao salva e mantida`() = runTest {
        store.save(Session("u1", "old", "ref1", expiresAt = now - 5))
        api.networkDown = true

        assertNull(repo.restoreSession())
        assertNotNull(store.load())
    }

    @Test
    fun `5xx e 429 no refresh mantem a sessao`() = runTest {
        store.save(Session("u1", "old", "ref1", expiresAt = now - 5))
        for (code in listOf(500, 429)) {
            api.refresh = HttpResult(code, "")
            assertNull(repo.accessToken())
            assertNotNull(store.load())
        }
    }

    @Test
    fun `chamadas concorrentes com o mesmo token rejeitado renovam so uma vez`() = runTest {
        store.save(Session("u1", "stale", "ref1", expiresAt = now + 3600))

        val results = List(5) { async { repo.accessToken(rejected = "stale") } }.awaitAll()

        assertEquals(1, api.refreshCalls)
        assertTrue(results.all { it == "acc2" })
    }

    @Test
    fun `logout apaga a sessao mesmo sem rede`() = runTest {
        store.save(Session("u1", "acc1", "ref1", expiresAt = now + 3600))
        api.networkDown = true

        repo.logout()

        assertNull(store.load())
        assertNull(repo.accessToken())
    }
}
