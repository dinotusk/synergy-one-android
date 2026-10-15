package br.com.synergyone.android.ui.leads

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.ApiErrors
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.api.LeadsPage
import br.com.synergyone.android.data.lead.LeadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun lead(id: Long) = LeadDto(id = id, nome = "Lead $id")

private class FakeLeads(private val pages: Map<String?, ApiResult<LeadsPage>>) : LeadRepository {
    val calls = mutableListOf<Pair<String?, String?>>() // etapa, cursor
    val empresas = mutableListOf<Long?>()
    override fun cached(id: Long): LeadDto? = null
    override suspend fun pagina(empresaId: Long?, etapa: String?, cursor: String?): ApiResult<LeadsPage> {
        calls += etapa to cursor
        empresas += empresaId
        return pages[cursor] ?: ApiResult.Error("sem página")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LeadsViewModelTest {
    private fun runVm(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }

    @Test
    fun `carrega a primeira pagina e depois acrescenta a proxima sem duplicar`() = runVm {
        val repo = FakeLeads(
            mapOf(
                null to ApiResult.Success(LeadsPage(listOf(lead(3), lead(2)), total = 4, proximoCursor = "c1")),
                "c1" to ApiResult.Success(LeadsPage(listOf(lead(2), lead(1)), total = 4, proximoCursor = null)),
            ),
        )
        val vm = LeadsViewModel(repo)
        advanceUntilIdle()
        assertEquals(listOf(3L, 2L), vm.state.value.leads.map { it.id })
        assertTrue(vm.state.value.canLoadMore)

        vm.loadMore()
        advanceUntilIdle()

        // lead 2 voltou na segunda página (a lista deslocou): não pode duplicar
        assertEquals(listOf(3L, 2L, 1L), vm.state.value.leads.map { it.id })
        assertNull(vm.state.value.nextCursor)
        assertFalse(vm.state.value.canLoadMore)
    }

    @Test
    fun `trocar a etapa recomeca da primeira pagina com o filtro`() = runVm {
        val repo = FakeLeads(mapOf(null to ApiResult.Success(LeadsPage(listOf(lead(1)), 1, "c1"))))
        val vm = LeadsViewModel(repo)
        advanceUntilIdle()

        vm.selectEtapa("ganho")
        advanceUntilIdle()

        assertEquals("ganho" to null, repo.calls.last()) // cursor da lista anterior não vaza para o filtro novo
        assertEquals("ganho", vm.state.value.etapa)
    }

    @Test
    fun `sessao invalida e marcada para o app voltar ao login`() = runVm {
        val repo = FakeLeads(mapOf(null to ApiResult.Error("Sua sessão expirou.", code = ApiErrors.SESSION_INVALID)))
        val vm = LeadsViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state.value.sessionExpired)
        assertEquals("Sua sessão expirou.", vm.state.value.error)
        assertTrue(vm.state.value.leads.isEmpty())
    }

    @Test
    fun `erro ao carregar mais mantem a lista ja carregada`() = runVm {
        val repo = FakeLeads(mapOf(null to ApiResult.Success(LeadsPage(listOf(lead(1)), 5, "c1"))))
        val vm = LeadsViewModel(repo)
        advanceUntilIdle()

        vm.loadMore() // "c1" não existe no fake: devolve erro
        advanceUntilIdle()

        assertEquals(1, vm.state.value.leads.size)
        assertEquals("sem página", vm.state.value.error)
        assertFalse(vm.state.value.isLoadingMore)
    }

    @Test
    fun `trocar a empresa recomeca com o filtro e mantem a etapa`() = runVm {
        val repo = FakeLeads(mapOf(null to ApiResult.Success(LeadsPage(listOf(lead(1)), 1, "c1"))))
        val vm = LeadsViewModel(repo)
        advanceUntilIdle()
        vm.selectEtapa("ganho")
        advanceUntilIdle()

        vm.selectEmpresa(7)
        advanceUntilIdle()

        assertEquals(7L, repo.empresas.last())
        assertEquals("ganho" to null, repo.calls.last())
        assertEquals(7L, vm.state.value.empresaId)

        vm.selectEmpresa(null) // "Todas as empresas"
        advanceUntilIdle()
        assertNull(repo.empresas.last())
    }
}
