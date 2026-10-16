package br.com.synergyone.android.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OkHttpSupabaseApiTest {
    private val server = MockWebServer().apply { start() }
    private val api = OkHttpSupabaseApi(server.url("/").toString(), anonKey = "anon-key")

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `login usa grant password com apikey e sem Authorization`() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        api.passwordGrant("a@b.com", "pw")

        val req = server.takeRequest()
        assertEquals("/auth/v1/token?grant_type=password", req.path)
        assertEquals("anon-key", req.getHeader("apikey"))
        assertNull(req.getHeader("Authorization"))
        assertEquals("""{"email":"a@b.com","password":"pw"}""", req.body.readUtf8())
    }

    @Test
    fun `logout e local`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        api.logout("jwt")

        val req = server.takeRequest()
        assertEquals("/auth/v1/logout?scope=local", req.path)
        assertEquals("Bearer jwt", req.getHeader("Authorization"))
    }
}
