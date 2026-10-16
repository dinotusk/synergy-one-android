package br.com.synergyone.android.ui.biblioteca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.ApiErrors
import br.com.synergyone.android.data.api.MaterialDto
import br.com.synergyone.android.data.biblioteca.BibliotecaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BibliotecaUiState(
    val area: String? = null,
    val empresaId: Long? = null,
    val itens: List<MaterialDto> = emptyList(),
    val total: Int = 0,
    val nextCursor: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoading && !isLoadingMore
}

class BibliotecaViewModel(private val repository: BibliotecaRepository) : ViewModel() {
    private val _state = MutableStateFlow(BibliotecaUiState())
    val state: StateFlow<BibliotecaUiState> = _state.asStateFlow()
    private var job: Job? = null

    init { reload() }

    fun selectArea(area: String?) {
        if (area == _state.value.area) return
        _state.update { it.copy(area = area) }
        reload()
    }

    fun selectEmpresa(id: Long?) {
        if (id == _state.value.empresaId) return
        _state.update { it.copy(empresaId = id) }
        reload()
    }

    /** Recomeça da primeira página (filtro novo ou "Tentar novamente"). */
    fun reload() {
        job?.cancel()
        _state.update { it.copy(isLoading = true, isLoadingMore = false, error = null, sessionExpired = false, itens = emptyList(), nextCursor = null) }
        val s = _state.value
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = s.empresaId, area = s.area)) {
                is ApiResult.Success -> _state.update { it.copy(isLoading = false, itens = r.data.itens, total = r.data.total, nextCursor = r.data.proximoCursor) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, error = r.message, sessionExpired = r.code == ApiErrors.SESSION_INVALID) }
            }
        }
    }

    /** Atualiza a primeira página mantendo a lista atual na tela; falha mantém o que já estava carregado. */
    fun refresh() {
        val s = _state.value
        if (s.isLoading || s.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, refreshFailed = false) }
        job?.cancel()
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = s.empresaId, area = s.area)) {
                is ApiResult.Success -> _state.update { it.copy(isRefreshing = false, itens = r.data.itens, total = r.data.total, nextCursor = r.data.proximoCursor, error = null) }
                is ApiResult.Error -> _state.update {
                    if (r.code == ApiErrors.SESSION_INVALID) it.copy(isRefreshing = false, error = r.message, sessionExpired = true)
                    else it.copy(isRefreshing = false, refreshFailed = true)
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        val cursor = s.nextCursor ?: return
        if (!s.canLoadMore) return
        _state.update { it.copy(isLoadingMore = true, error = null) }
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = s.empresaId, area = s.area, cursor = cursor)) {
                is ApiResult.Success -> _state.update {
                    val known = it.itens.mapTo(HashSet()) { m -> m.id }
                    it.copy(isLoadingMore = false, itens = it.itens + r.data.itens.filter { m -> m.id !in known }, total = r.data.total, nextCursor = r.data.proximoCursor)
                }
                is ApiResult.Error -> _state.update { it.copy(isLoadingMore = false, error = r.message, sessionExpired = r.code == ApiErrors.SESSION_INVALID) }
            }
        }
    }

    class Factory(private val repository: BibliotecaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BibliotecaViewModel(repository) as T
    }
}
