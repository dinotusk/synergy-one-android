package br.com.synergyone.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import br.com.synergyone.android.core.PerfLog
import br.com.synergyone.android.data.freshness.DataFreshness
import kotlinx.coroutines.Job
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

/**
 * Carrega um recurso da API. Três formas de recarregar:
 *  - [reload]: do zero, com carregamento visível (primeira carga, "Tentar novamente", chave nova);
 *  - [refresh]: em segundo plano; o dado atual continua na tela e só é trocado quando o novo chega
 *    (puxar para atualizar). Se falhar, mantém o dado antigo e marca [refreshFailed];
 *  - [refreshIfStale]: o mesmo, mas só se houve gravação desde a leitura ou passou o TTL.
 */
class LoaderViewModel<T>(
    private val loader: suspend () -> ApiResult<T>,
    private val freshness: DataFreshness? = null,
    private val label: String = "",
) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<T>>(LoadState.Loading)
    val state: StateFlow<LoadState<T>> = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private val _refreshFailed = MutableStateFlow(false)
    val refreshFailed: StateFlow<Boolean> = _refreshFailed.asStateFlow()

    private var loadedAt = 0L
    private var loadedVersion = 0L
    private var job: Job? = null

    init { reload() }

    fun reload() {
        job?.cancel()
        _state.value = LoadState.Loading
        _refreshFailed.value = false
        run(silent = false)
    }

    fun refresh() {
        if (_state.value !is LoadState.Loaded) return reload()
        if (_refreshing.value) return
        run(silent = true)
    }

    fun refreshIfStale() {
        val f = freshness ?: return
        if (_state.value is LoadState.Loaded && !_refreshing.value && f.isStale(loadedVersion, loadedAt)) run(silent = true)
    }

    private fun run(silent: Boolean) {
        val versionAtStart = freshness?.version?.value ?: 0L
        val startedAt = System.nanoTime()
        if (silent) { _refreshing.value = true; _refreshFailed.value = false }
        job = viewModelScope.launch {
            val r = loader()
            PerfLog.d("carga ${label.ifBlank { "tela" }} ${if (silent) "(2º plano)" else ""} ${(System.nanoTime() - startedAt) / 1_000_000}ms ${if (r is ApiResult.Success) "ok" else "erro"}")
            when (r) {
                is ApiResult.Success -> {
                    loadedAt = freshness?.now() ?: System.currentTimeMillis()
                    loadedVersion = versionAtStart
                    _state.value = LoadState.Loaded(r.data)
                }
                is ApiResult.Error ->
                    if (silent && r.code != ApiErrors.SESSION_INVALID) _refreshFailed.value = true
                    else _state.value = LoadState.Error(r.message, r.code == ApiErrors.SESSION_INVALID)
            }
            _refreshing.value = false
        }
    }
}

/** Um [LoaderViewModel] por [key]: mudar a chave (ex.: outra empresa) dispara um novo carregamento. */
@Composable
@Suppress("UNCHECKED_CAST")
fun <T> rememberLoader(key: String, loader: suspend () -> ApiResult<T>): LoaderViewModel<T> {
    val freshness = LocalDataFreshness.current
    val vm = viewModel<LoaderViewModel<*>>(
        key = key,
        factory = viewModelFactory { initializer { LoaderViewModel(loader, freshness, key.replace(Regex("\\d+"), "N")) } },
    ) as LoaderViewModel<T>
    // Ao entrar na composição (voltar à aba/tela) e ao voltar ao app: atualiza só se houver motivo.
    LaunchedEffect(vm) { vm.refreshIfStale() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshIfStale() }
    return vm
}

/** Entregue na raiz pela navegação; ausente (null) nos previews e testes. */
val LocalDataFreshness = staticCompositionLocalOf<DataFreshness?> { null }
