package br.com.synergyone.android.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.ApiErrors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Error(val message: String, val sessionExpired: Boolean) : LoadState<Nothing>
    data class Loaded<T>(val data: T) : LoadState<T>
}

/** Carrega um recurso da API e permite recarregar (pull/botão "Tentar novamente"). */
class LoaderViewModel<T>(private val loader: suspend () -> ApiResult<T>) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<T>>(LoadState.Loading)
    val state: StateFlow<LoadState<T>> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        _state.value = LoadState.Loading
        viewModelScope.launch {
            _state.value = when (val r = loader()) {
                is ApiResult.Success -> LoadState.Loaded(r.data)
                is ApiResult.Error -> LoadState.Error(r.message, r.code == ApiErrors.SESSION_INVALID)
            }
        }
    }
}

/** Um [LoaderViewModel] por [key]: mudar a chave (ex.: outra empresa) dispara um novo carregamento. */
@Composable
@Suppress("UNCHECKED_CAST")
fun <T> rememberLoader(key: String, loader: suspend () -> ApiResult<T>): LoaderViewModel<T> =
    viewModel<LoaderViewModel<*>>(
        key = key,
        factory = viewModelFactory { initializer { LoaderViewModel(loader) } },
    ) as LoaderViewModel<T>
