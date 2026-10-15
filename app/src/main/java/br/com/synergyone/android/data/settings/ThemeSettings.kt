package br.com.synergyone.android.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { Dark, Light }

/**
 * Preferência de aparência. O padrão é **escuro** (identidade do produto), independente do tema do
 * celular; o claro é opção do usuário. Não é dado sensível: SharedPreferences comum.
 */
class ThemeSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("synergy_prefs", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
        _mode.value = mode
    }

    private fun read(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY, null) } ?: ThemeMode.Dark

    private companion object { const val KEY = "theme_mode" }
}
