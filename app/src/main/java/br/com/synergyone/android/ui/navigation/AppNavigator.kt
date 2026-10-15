package br.com.synergyone.android.ui.navigation

/** O que as abas podem pedir à navegação (telas de detalhe vivem como rotas próprias). */
interface AppNavigator {
    fun openPerfil()
    fun openEmpresa(id: Long)
    fun openLead(id: Long)
    fun openLeadsDaEmpresa(id: Long)
    fun openTermometroDaEmpresa(id: Long)
    fun openWeb(path: String)
    fun back()
}
