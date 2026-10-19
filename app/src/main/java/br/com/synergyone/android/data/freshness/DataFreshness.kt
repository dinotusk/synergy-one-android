package br.com.synergyone.android.data.freshness

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controla quando um dado já carregado deixa de valer. Dois motivos:
 *  1. **Alteração**: qualquer gravação bem-sucedida na API ([invalidate]) torna velho tudo o que foi lido antes.
 *  2. **Tempo**: passado [ttlMillis] sem atualizar, o dado é considerado velho ao reabrir a tela/aba.
 * Dado velho não some: continua na tela enquanto a atualização acontece (ver `LoaderViewModel.refreshIfStale`).
 */
class DataFreshness(
    private val now: () -> Long = System::currentTimeMillis,
    val ttlMillis: Long = DEFAULT_TTL_MILLIS,
) {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    fun invalidate() { _version.value = _version.value + 1 }

    fun isStale(loadedVersion: Long, loadedAtMillis: Long): Boolean =
        loadedVersion != _version.value || now() - loadedAtMillis > ttlMillis

    fun now(): Long = now.invoke()

    companion object { const val DEFAULT_TTL_MILLIS = 5 * 60 * 1000L }
}
