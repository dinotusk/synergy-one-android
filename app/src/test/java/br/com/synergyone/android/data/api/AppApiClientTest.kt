package br.com.synergyone.android.data.api

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.auth.AuthRepository
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.empresa.ApiEmpresaRepository
import br.com.synergyone.android.data.lead.ApiLeadRepository
import br.com.synergyone.android.data.remote.HttpResult
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAuth(var token: String? = "t1", val renewed: String? = "t2") : AuthRepository {
    var rejectedSeen: String? = null
    override val currentUser: StateFlow<AuthenticatedUser?> = MutableStateFlow(null)
    override suspend fun login(email: String, password: String): ApiResult<AuthenticatedUser> = error("n/a")
    override suspend fun restoreSession(): AuthenticatedUser? = null
    override suspend fun logout() = Unit
    override suspend fun accessToken(rejected: String?): String? {
        if (rejected == null) return token
        rejectedSeen = rejected
        return renewed
    }
}

private class FakeApi(val respond: (String) -> HttpResult) : AppApi {
    val tokensUsed = mutableListOf<String>()
    var lastQuery: Map<String, String> = emptyMap()
    override suspend fun request(method: String, path: String, token: String, query: Map<String, String>, body: String?): HttpResult {
        tokensUsed += token
        lastQuery = query
        return respond(token)
    }
}

class AppApiClientTest {
    private val empresasBody = """{"empresas":[{"id":3,"nome":"Acme","slug":"acme","plano":"pro","status":"ativo",
        "modulos":{"leads":true,"midias":true,"api":false},"novoCampo":1}]}"""

    private fun client(auth: FakeAuth = FakeAuth(), respond: (String) -> HttpResult) =
        AppApiClient(FakeApi(respond), auth)

    @Test
    fun `lista empresas e deriva modulos ativos`() = runTest {
        val repo = ApiEmpresaRepository(client { HttpResult(200, empresasBody) })
        val e = (repo.listar() as ApiResult.Success).data.single()
        assertEquals("Acme", e.nome)
        assertEquals(listOf("leads", "midias"), e.modulosAtivos)
    }

    @Test
    fun `401 renova o token uma vez e refaz a chamada`() = runTest {
        val api = FakeApi { if (it == "t1") HttpResult(401, "") else HttpResult(200, empresasBody) }
        val auth = FakeAuth()
        val result = ApiEmpresaRepository(AppApiClient(api, auth)).listar()

        assertTrue(result is ApiResult.Success)
        assertEquals(listOf("t1", "t2"), api.tokensUsed)
        assertEquals("t1", auth.rejectedSeen)
    }

    @Test
    fun `continua 401 depois de renovar vira sessao expirada sem loop`() = runTest {
        val api = FakeApi { HttpResult(401, "") }
        val r = AppApiClient(api, FakeAuth()).getRaw("/x") as ApiResult.Error

        assertEquals(ApiErrors.SESSION_INVALID, r.code)
        assertEquals(2, api.tokensUsed.size)
    }

    @Test
    fun `sem sessao nem chama a rede`() = runTest {
        val api = FakeApi { HttpResult(200, "{}") }
        val r = AppApiClient(api, FakeAuth(token = null)).getRaw("/x") as ApiResult.Error
        assertEquals(ApiErrors.SESSION_INVALID, r.code)
        assertTrue(api.tokensUsed.isEmpty())
    }

    @Test
    fun `erros do contrato viram mensagens e codigos`() = runTest {
        fun err(code: Int, body: String) = client { HttpResult(code, body) }
        suspend fun AppApiClient.erro() = getRaw("/x") as ApiResult.Error

        // 403 sem_permissao / 404 / 409 / 422: usa a frase pronta do servidor
        val perm = err(403, """{"error":"sem_permissao","message":"Seu papel não permite gravar."}""").erro()
        assertEquals("Seu papel não permite gravar.", perm.message)
        assertEquals(ApiErrors.NO_PERMISSION, perm.code)
        assertEquals("Lead não encontrado.", err(404, """{"error":"nao_encontrado","message":"Lead não encontrado."}""").erro().message)

        assertTrue(err(403, """{"error":"sem_acesso","motivo":"inativo"}""").erro().message.contains("suspenso"))
        assertTrue(err(429, "").erro().message.contains("Muitas"))
        // 503 é infraestrutura: mensagem de indisponibilidade, nunca de acesso negado
        val down = err(503, """{"error":"indisponivel"}""").erro()
        assertEquals(ApiErrors.UNAVAILABLE, down.code)
        assertTrue(down.message.contains("indispon"))
        assertTrue(err(500, "<html>").erro().message.contains("indispon"))
    }

    @Test
    fun `rede fora e resposta invalida`() = runTest {
        val offline = AppApiClient(object : AppApi {
            override suspend fun request(method: String, path: String, token: String, query: Map<String, String>, body: String?): HttpResult =
                throw IOException()
        }, FakeAuth())
        assertEquals(ApiErrors.NETWORK, (offline.getRaw("/x") as ApiResult.Error).code)

        assertTrue(ApiEmpresaRepository(client { HttpResult(200, "<html>") }).listar() is ApiResult.Error)
    }

    @Test
    fun `leads - etapa captado nao e filtravel e o cursor e repassado`() = runTest {
        val api = FakeApi { HttpResult(200, """{"leads":[{"id":1,"nome":"Carlos","etapa":"proposta"}],"total":1,"proximoCursor":"abc"}""") }
        val repo = ApiLeadRepository(AppApiClient(api, FakeAuth()))

        val page = (repo.pagina(etapa = "captado", cursor = "xyz") as ApiResult.Success).data
        assertNull(api.lastQuery["etapa"])
        assertEquals("xyz", api.lastQuery["cursor"])
        assertEquals("abc", page.proximoCursor)
        assertEquals("proposta", page.leads.single().etapa)

        repo.pagina(etapa = "ganho")
        assertEquals("ganho", api.lastQuery["etapa"])
    }
}

class JornadaERepositoriosTest {
    private fun client(respond: (String, String, Map<String, String>, String?) -> HttpResult) = AppApiClient(object : AppApi {
        override suspend fun request(method: String, path: String, token: String, query: Map<String, String>, body: String?) =
            respond(method, path, query, body)
    }, FakeAuth())

    private val pagina = """{"leads":[{"id":1204,"empresaId":3,"etapa":"proposta","responsavelId":"r1","campanhaId":7,"propostaCentavos":150000}],"total":1,"proximoCursor":null,"podeEditar":true,"responsaveis":[{"id":"r1","nome":"Ana"}],"campanhas":[{"id":7,"empresaId":3,"nome":"Outubro"}]}"""

    @Test
    fun `acha o estado do lead pela listagem do dia da captura em Brasilia`() = runTest {
        var query: Map<String, String> = emptyMap()
        val repo = br.com.synergyone.android.data.jornada.ApiJornadaRepository(client { _, _, q, _ -> query = q; HttpResult(200, pagina) })

        // 02:00 UTC do dia 8 ainda é dia 7 em Brasília (UTC-3)
        val r = (repo.estadoDoLead(1204, 3, "2026-10-08T02:00:00.000Z") as ApiResult.Success).data!!

        assertEquals("proposta", r.lead.etapa)
        assertEquals("2026-10-07", query["desde"]); assertEquals("2026-10-07", query["ate"]); assertEquals("3", query["empresaId"])
        assertTrue(r.apoio.podeEditar)
    }

    @Test
    fun `lead fora da listagem devolve nulo e nao inventa estado`() = runTest {
        val repo = br.com.synergyone.android.data.jornada.ApiJornadaRepository(client { _, _, _, _ -> HttpResult(200, """{"leads":[],"total":0,"proximoCursor":null}""") })
        assertNull((repo.estadoDoLead(999, 3, "2026-10-07T14:00:00.000Z") as ApiResult.Success).data)
    }

    @Test
    fun `salvar manda PUT com o estado completo e nulos explicitos`() = runTest {
        var method = ""; var path = ""; var body: String? = null
        val repo = br.com.synergyone.android.data.jornada.ApiJornadaRepository(client { m, p, _, b -> method = m; path = p; body = b; HttpResult(204, "") })
        val req = br.com.synergyone.android.data.jornada.JornadaForm(etapa = "contato").toRequest()

        assertTrue(repo.salvar(1204, req) is ApiResult.Success)

        assertEquals("PUT", method); assertEquals("/api/app/v1/equipe/jornada/1204", path)
        assertTrue(body!!.contains("\"vendaValor\":null") && body!!.contains("\"etapa\":\"contato\""))
    }

    @Test
    fun `422 devolve o campo e a mensagem do servidor`() = runTest {
        val repo = br.com.synergyone.android.data.jornada.ApiJornadaRepository(client { _, _, _, _ ->
            HttpResult(422, """{"error":"dados_invalidos","message":"Informe o valor da venda.","campo":"vendaValor"}""")
        })
        val e = repo.salvar(1, br.com.synergyone.android.data.jornada.JornadaForm(etapa = "contato").toRequest()) as ApiResult.Error
        assertEquals("vendaValor", e.field); assertEquals("Informe o valor da venda.", e.message)
    }

    @Test
    fun `termometro cria com POST e edita com PUT e le o id`() = runTest {
        val calls = mutableListOf<Pair<String, String>>()
        val repo = br.com.synergyone.android.data.termometro.ApiTermometroRepository(client { m, p, _, _ -> calls += m to p; HttpResult(if (m == "POST") 201 else 200, """{"id":19}""") })
        val req = br.com.synergyone.android.data.termometro.AvaliacaoForm(3, null, "2026-10-01", "2026-10-31", "verde", "j", "f").toRequest()

        assertEquals(19L, (repo.salvarAvaliacao(null, req) as ApiResult.Success).data)
        assertEquals(19L, (repo.salvarAvaliacao(19, req) as ApiResult.Success).data)
        repo.publicar(19)

        assertEquals(
            listOf("POST" to "/api/app/v1/equipe/termometro/avaliacoes", "PUT" to "/api/app/v1/equipe/termometro/avaliacoes/19", "POST" to "/api/app/v1/equipe/termometro/avaliacoes/19/publicar"),
            calls,
        )
    }

    @Test
    fun `publicar sem permissao mostra a mensagem do servidor sem deslogar`() = runTest {
        val repo = br.com.synergyone.android.data.termometro.ApiTermometroRepository(client { _, _, _, _ ->
            HttpResult(403, """{"error":"sem_permissao","message":"Só administrador ou gestão publica."}""")
        })
        val e = repo.publicar(19) as ApiResult.Error
        assertEquals(ApiErrors.NO_PERMISSION, e.code); assertEquals("Só administrador ou gestão publica.", e.message)
    }
}
