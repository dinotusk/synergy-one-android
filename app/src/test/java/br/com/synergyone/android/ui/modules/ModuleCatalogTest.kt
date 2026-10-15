package br.com.synergyone.android.ui.modules

import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.Papel
import br.com.synergyone.android.data.auth.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleCatalogTest {
    private fun team(papel: Papel) = AuthenticatedUser("1", "Ana", "a@x.com", UserRole.TEAM, papel, todasEmpresas = true)
    private val cliente = AuthenticatedUser("2", "Bia", "b@x.com", UserRole.CLIENT, Papel.CLIENTE)
    private fun ids(u: AuthenticatedUser, hab: Set<String>? = null) = ModuleCatalog.visibleFor(u, hab).map { it.id }

    @Test
    fun `financeiro e gestao so para admin ou gestao`() {
        for (p in listOf(Papel.ADMIN, Papel.GESTAO)) {
            assertTrue("financeiro" in ids(team(p)))
            assertTrue("gestao" in ids(team(p)))
        }
        for (p in listOf(Papel.OPERACAO, Papel.LEITOR)) {
            assertFalse("financeiro" in ids(team(p)))
            assertFalse("gestao" in ids(team(p)))
        }
    }

    @Test
    fun `chaves de API so para administrador`() {
        assertTrue("api-chaves" in ids(team(Papel.ADMIN)))
        assertFalse("api-chaves" in ids(team(Papel.GESTAO)))
    }

    @Test
    fun `leitor ve o que a equipe le mas nenhum modulo restrito`() {
        val leitor = ids(team(Papel.LEITOR))
        assertTrue("producao" in leitor && "equipe" in leitor && "leads" in leitor)
    }

    @Test
    fun `cliente nunca ve modulos da equipe`() {
        val c = ids(cliente)
        assertFalse(c.any { it in setOf("financeiro", "gestao", "automacoes", "producao", "api-chaves", "equipe") })
        assertTrue("termometro" in c && "leads" in c)
    }

    @Test
    fun `modulos do cliente dependem do que a empresa habilitou`() {
        val sem = ids(cliente, setOf("leads"))
        assertFalse("pre-analise" in sem || "midias" in sem || "api-webhooks" in sem)
        val com = ids(cliente, setOf("leads", "preAnalise", "midias"))
        assertTrue("pre-analise" in com && "midias" in com)
        assertFalse("api-webhooks" in com)
        // biblioteca e configuracoes sao sempre do cliente
        assertTrue("biblioteca" in sem && "configuracoes" in sem)
    }

    @Test
    fun `todo modulo web aponta para um caminho do site e nenhum expoe segredo`() {
        val web = (ModuleCatalog.team + ModuleCatalog.client).mapNotNull { (it.target as? ModuleTarget.Web)?.path }
        assertTrue(web.all { it.startsWith("/") })
        assertEquals(web.size, web.filter { !it.contains("token") && !it.contains("key") && !it.contains("?") }.size)
    }
}
