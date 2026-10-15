package br.com.synergyone.android.ui.empresas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.empresa.Empresa
import br.com.synergyone.android.data.empresa.EmpresaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EmpresasUiState {
    data object Loading : EmpresasUiState
    data class Error(val message: String) : EmpresasUiState
    data class Loaded(val empresas: List<Empresa>) : EmpresasUiState
}

class EmpresasViewModel(private val repository: EmpresaRepository) : ViewModel() {
    private val _state = MutableStateFlow<EmpresasUiState>(EmpresasUiState.Loading)
    val state: StateFlow<EmpresasUiState> = _state.asStateFlow()

    init { carregar() }

    fun carregar() {
        _state.value = EmpresasUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = repository.listar()) {
                is ApiResult.Success -> EmpresasUiState.Loaded(r.data)
                is ApiResult.Error -> EmpresasUiState.Error(r.message)
            }
        }
    }

    class Factory(private val repository: EmpresaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EmpresasViewModel(repository) as T
    }
}
