package br.com.synergyone.android.data.api

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OkHttpAppApiTest {
    private val server = MockWebServer().apply { start() }
    private val api = OkHttpAppApi(server.url("/").toString())

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `usa so Bearer, sem apikey, e devolve o x-request-id`() = runTest {
        server.enqueue(MockResponse().setBody("{}").setHeader("x-request-id", "req-1"))
        val r = api.request("GET", "/api/app/v1/leads", "jwt", mapOf("limite" to "30", "cursor" to "a b"))

        val req = server.takeRequest()
        assertEquals("/api/app/v1/leads?limite=30&cursor=a%20b", req.path)
        assertEquals("Bearer jwt", req.getHeader("Authorization"))
        assertNull(req.getHeader("apikey"))
        assertEquals("req-1", r.requestId)
    }

    @Test
    fun `PUT envia o corpo JSON`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        api.request("PUT", "/api/app/v1/equipe/jornada/9", "jwt", body = """{"etapa":"ganho"}""")

        val req = server.takeRequest()
        assertEquals("PUT", req.method)
        assertEquals("""{"etapa":"ganho"}""", req.body.readUtf8())
    }
}
