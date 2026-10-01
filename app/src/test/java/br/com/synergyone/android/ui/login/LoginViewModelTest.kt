package br.com.synergyone.android.ui.login

import br.com.synergyone.android.data.auth.PendingAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Test
    fun `login never succeeds while backend integration is pending`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = LoginViewModel(PendingAuthRepository())

            viewModel.onUsernameChange("maria")
            viewModel.onPasswordChange("123456")
            viewModel.onLoginClick()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull("deve expor uma mensagem de erro, nunca simular sucesso", state.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `submit is disabled until both fields are filled`() {
        val viewModel = LoginViewModel(PendingAuthRepository())

        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onUsernameChange("maria")
        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onPasswordChange("123456")
        assertEquals(true, viewModel.uiState.value.isSubmitEnabled)
    }
}
