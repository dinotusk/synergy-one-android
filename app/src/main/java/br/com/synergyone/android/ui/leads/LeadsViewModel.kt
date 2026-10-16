package br.com.synergyone.android.ui.leads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.ApiErrors
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.freshness.DataFreshness
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
    /** Janela de captura em dias (7, 30, 90) ou `null` = todo o período. A API só aceita `desde`, sem data final. */
    val dias: Int? = null,
    /** `desde` (`AAAA-MM-DD`, Brasília) fixado ao escolher a janela, para reload, atualização e "carregar mais" usarem o mesmo corte. */
    val desde: String? = null,
    val leads: List<LeadDto> = emptyList(),
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

/** Primeiro dia de uma janela de [dias] dias que termina hoje (Brasília), como os períodos do Termômetro. */
internal fun desdeDe(dias: Int, hoje: java.time.LocalDate = br.com.synergyone.android.ui.termometro.hojeBrasilia()): String =
    hoje.minusDays(dias - 1L).toString()

class LeadsViewModel(
    private val repository: LeadRepository,
    initialEmpresaId: Long? = null,
    private val freshness: DataFreshness? = null,
) : ViewModel() {
    private var loadedAt = 0L
    private var loadedVersion = 0L
    private val _state = MutableStateFlow(LeadsUiState(empresaId = initialEmpresaId))
    val state: StateFlow<LeadsUiState> = _state.asStateFlow()
    private var job: Job? = null

    init { reload() }

    fun selectEmpresa(id: Long?) {
        if (id == _state.value.empresaId) return
        _state.update { it.copy(empresaId = id) }
        reload()
    }

    fun selectPeriodo(dias: Int?) {
        if (dias == _state.value.dias) return
        _state.update { it.copy(dias = dias, desde = dias?.let { d -> desdeDe(d) }) }
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
        val version = freshness?.version?.value ?: 0L
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = empresaId, etapa = etapa, desde = _state.value.desde)) {
                is ApiResult.Success -> _state.update {
                    markLoaded(version)
                    it.copy(isLoading = false, leads = r.data.leads, total = r.data.total, nextCursor = r.data.proximoCursor)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, error = r.message, sessionExpired = r.code == ApiErrors.SESSION_INVALID)
                }
            }
        }
    }

    /** Atualiza a primeira página em segundo plano: a lista atual fica na tela até a nova chegar. */
    fun refresh() {
        val s = _state.value
        if (s.isLoading || s.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, refreshFailed = false) }
        val version = freshness?.version?.value ?: 0L
        job?.cancel()
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = s.empresaId, etapa = s.etapa, desde = s.desde)) {
                is ApiResult.Success -> {
                    markLoaded(version)
                    _state.update { it.copy(isRefreshing = false, leads = r.data.leads, total = r.data.total, nextCursor = r.data.proximoCursor, error = null) }
                }
                is ApiResult.Error -> _state.update {
                    if (r.code == ApiErrors.SESSION_INVALID) it.copy(isRefreshing = false, error = r.message, sessionExpired = true)
                    else it.copy(isRefreshing = false, refreshFailed = true)
                }
            }
        }
    }

    /** Ao reabrir a aba/tela: só atualiza se houve gravação desde a leitura ou passou o TTL. */
    fun refreshIfStale() {
        val f = freshness ?: return
        val s = _state.value
        if (s.isLoading || s.error != null || s.isRefreshing || s.isLoadingMore) return
        if (f.isStale(loadedVersion, loadedAt)) refresh()
    }

    private fun markLoaded(version: Long) {
        loadedAt = freshness?.now() ?: System.currentTimeMillis()
        loadedVersion = version
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (!current.canLoadMore) return
        _state.update { it.copy(isLoadingMore = true, error = null) }
        job = viewModelScope.launch {
            when (val r = repository.pagina(empresaId = current.empresaId, etapa = current.etapa, cursor = cursor, desde = current.desde)) {
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

    class Factory(private val repository: LeadRepository, private val empresaId: Long? = null, private val freshness: DataFreshness? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LeadsViewModel(repository, empresaId, freshness) as T
    }
}
