package br.com.synergyone.android.ui.leads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.ApiErrors
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.lead.LeadRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeadsUiState(
    val etapa: String? = null,
    val empresaId: Long? = null,
    val leads: List<LeadDto> = emptyList(),
    val total: Int = 0,
    val nextCursor: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoading && !isLoadingMore
}

class LeadsViewModel(private val repository: LeadRepository, initialEmpresaId: Long? = null) : ViewModel() {
    private val _state = MutableStateFlow(LeadsUiState(empresaId = initialEmpresaId))
    val state: StateFlow<LeadsUiState> = _state.asStateFlow()
    private var job: Job? = null

    init { reload() }

    fun selectEmpresa(id: Long?) {
        if (id == _state.value.empresaId) return
        _state.update { it.copy(empresaId = id) }
        reload()
    }

    fun selectEtapa(etapa: String?) {
        if (etapa == _state.value.etapa) return
        _state.update { it.copy(etapa = etapa) }
        reload()
    }

    /** Recomeça da primeira página (filtro novo ou "Tentar novamente"). */
    fun reload() {
        job?.cancel()
        _state.update { it.copy(isLoading = true, isLoadingMore = false, error = null, sessionExpired = false, leads = emptyList(), nextCursor = null) }
        val etapa = _state.value.etapa
        val empresaId = _state.value.empresaId
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = empresaId, etapa = etapa)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isLoading = false, leads = r.data.leads, total = r.data.total, nextCursor = r.data.proximoCursor)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, error = r.message, sessionExpired = r.code == ApiErrors.SESSION_INVALID)
                }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (!current.canLoadMore) return
        _state.update { it.copy(isLoadingMore = true, error = null) }
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = current.empresaId, etapa = current.etapa, cursor = cursor)) {
                is ApiResult.Success -> _state.update {
                    // Dedup por id: a API ordena por captura e um lead novo pode deslocar a página.
                    val known = it.leads.mapTo(HashSet()) { l -> l.id }
                    it.copy(
                        isLoadingMore = false,
                        leads = it.leads + r.data.leads.filter { l -> l.id !in known },
                        total = r.data.total,
                        nextCursor = r.data.proximoCursor,
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoadingMore = false, error = r.message, sessionExpired = r.code == ApiErrors.SESSION_INVALID)
                }
            }
        }
    }

    class Factory(private val repository: LeadRepository, private val empresaId: Long? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LeadsViewModel(repository, empresaId) as T
    }
}
