package br.com.synergyone.android.ui.common

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.freshness.DataFreshness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoaderViewModelTest {
    private var agora = 0L
    private val freshness = DataFreshness(now = { agora }, ttlMillis = 300_000)
    private var chamadas = 0
    private var resposta: ApiResult<String> = ApiResult.Success("v1")

    private fun runVm(block: suspend kotlinx.coroutines.test.TestScope.(LoaderViewModel<String>) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = LoaderViewModel({ chamadas++; resposta }, freshness)
            advanceUntilIdle()
            block(vm)
        } finally { Dispatchers.resetMain() }
    }

    private fun LoaderViewModel<String>.dado() = (state.value as? LoadState.Loaded)?.data

    @Test fun `carrega uma vez e nao refaz a consulta sem motivo`() = runVm { vm ->
        assertEquals("v1", vm.dado())
        vm.refreshIfStale(); advanceUntilIdle()
        assertEquals(1, chamadas)
    }

    @Test fun `gravacao depois da leitura faz atualizar, mantendo o dado antigo durante a atualizacao`() = runVm { vm ->
        freshness.invalidate()
        resposta = ApiResult.Success("v2")
        vm.refreshIfStale()
        assertEquals("v1", vm.dado())          // continua na tela
        assertTrue(vm.refreshing.value)
        advanceUntilIdle()
        assertEquals("v2", vm.dado())
        assertFalse(vm.refreshing.value)
        vm.refreshIfStale(); advanceUntilIdle()
        assertEquals(2, chamadas)               // nao repete: ja esta em dia
    }

    @Test fun `passado o TTL atualiza ao reabrir`() = runVm { vm ->
        agora += 300_001
        resposta = ApiResult.Success("v2")
        vm.refreshIfStale(); advanceUntilIdle()
        assertEquals("v2", vm.dado())
    }

    @Test fun `falha ao atualizar mantem o dado antigo e avisa`() = runVm { vm ->
        resposta = ApiResult.Error("sem rede", code = "rede")
        vm.refresh(); advanceUntilIdle()
        assertEquals("v1", vm.dado())
        assertTrue(vm.refreshFailed.value)
    }

    @Test fun `sessao invalida ao atualizar nao esconde atras de dado antigo`() = runVm { vm ->
        resposta = ApiResult.Error("expirou", code = "sessao_invalida")
        vm.refresh(); advanceUntilIdle()
        assertTrue(vm.state.value is LoadState.Error)
    }

    @Test fun `tentar novamente recarrega do zero`() = runVm { vm ->
        resposta = ApiResult.Success("v3")
        vm.reload()
        assertTrue(vm.state.value is LoadState.Loading)
        advanceUntilIdle()
        assertEquals("v3", vm.dado())
    }
}
