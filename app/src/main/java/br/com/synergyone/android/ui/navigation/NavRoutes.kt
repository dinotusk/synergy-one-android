package br.com.synergyone.android.ui.navigation

/** Rotas de navegação do app. A área acessível depende do papel (equipe/cliente) do usuário autenticado. */
object NavRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val TEAM_HOME = "team_home"
    const val CLIENT_HOME = "client_home"
    const val PERFIL = "perfil"

    const val ARG_ID = "id"
    const val EMPRESA = "empresa/{$ARG_ID}"
    const val EMPRESA_LEADS = "empresa/{$ARG_ID}/leads"
    const val EMPRESA_TERMOMETRO = "empresa/{$ARG_ID}/termometro"
    const val LEAD = "lead/{$ARG_ID}"

    fun empresa(id: Long) = "empresa/$id"
    fun empresaLeads(id: Long) = "empresa/$id/leads"
    fun empresaTermometro(id: Long) = "empresa/$id/termometro"
    fun lead(id: Long) = "lead/$id"
}
