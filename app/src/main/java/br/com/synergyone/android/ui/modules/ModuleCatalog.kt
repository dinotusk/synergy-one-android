package br.com.synergyone.android.ui.modules

import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.UserRole

/** Categorias do menu do site (`navGroups`), na mesma ordem. */
enum class ModuleCategory(val label: String) {
    Inicio("Início"),
    ClientesVendas("Clientes e vendas"),
    Operacao("Operação"),
    Inteligencia("Inteligência"),
    Conteudo("Conteúdo"),
    Gestao("Gestão"),
    Configuracoes("Configurações"),
}

/**
 * Quem pode usar o módulo, espelhando o **servidor** (não a matriz de demonstração do menu do site):
 * `Membro` = equipe ativa (inclui leitor) · `Admin` = admin ou gestão · `Administrador` = só admin ·
 * `Cliente` = conta de cliente (restrita às empresas dele pela RLS).
 */
enum class ModuleAccess {
    Membro, Admin, Administrador, Cliente;

    fun allows(user: AuthenticatedUser): Boolean = when (this) {
        Membro -> user.role == UserRole.TEAM
        Admin -> user.isAdminOrManager
        Administrador -> user.isAdmin
        Cliente -> user.role == UserRole.CLIENT
    }
}

/** Onde o módulo vive dentro do app. */
sealed interface ModuleTarget {
    /** Funciona dentro do app (aba ou tela própria). */
    data class Native(val tab: NativeTab) : ModuleTarget

    /** Só existe no site: o app abre a página no navegador (o login do site é separado). */
    data class Web(val path: String) : ModuleTarget
}

enum class NativeTab { Termometro, Clientes, Leads, Biblioteca }

data class AppModule(
    val id: String,
    val label: String,
    val description: String,
    val category: ModuleCategory,
    val access: ModuleAccess,
    val target: ModuleTarget,
    /** Chave do módulo em `empresa.modulos` que habilita este item para clientes; `null` = sempre. */
    val empresaModulo: String? = null,
)

private fun web(id: String, label: String, description: String, category: ModuleCategory, access: ModuleAccess, path: String, modulo: String? = null) =
    AppModule(id, label, description, category, access, ModuleTarget.Web(path), modulo)

private fun native(id: String, label: String, description: String, category: ModuleCategory, access: ModuleAccess, tab: NativeTab) =
    AppModule(id, label, description, category, access, ModuleTarget.Native(tab))

/**
 * Inventário de módulos (ver docs/COBERTURA-FUNCIONAL.md). Módulo sem API do backend não é
 * reimplementado no app: aparece como "Na web" e abre a página correspondente.
 */
object ModuleCatalog {
    val team: List<AppModule> = listOf(
        native("termometro", "Termômetro", "Avaliação e resultados por empresa e período.", ModuleCategory.Inicio, ModuleAccess.Membro, NativeTab.Termometro),
        web("visao-geral", "Visão geral", "Clientes, leads abertos, projetos e prioridades.", ModuleCategory.Inicio, ModuleAccess.Membro, "/"),
        web("meu-dia", "Meu dia", "Prioridades pessoais pelos prazos e responsáveis.", ModuleCategory.Inicio, ModuleAccess.Membro, "/meu-dia"),

        native("clientes", "Empresas e ficha", "Carteira de empresas com resultados e leads.", ModuleCategory.ClientesVendas, ModuleAccess.Membro, NativeTab.Clientes),
        native("leads", "Leads e vendas", "Leads captados, jornada, notas e recebimentos.", ModuleCategory.ClientesVendas, ModuleAccess.Membro, NativeTab.Leads),
        web("carteira", "Carteira e Cliente 360", "Saúde, prazos e histórico completo do cliente.", ModuleCategory.ClientesVendas, ModuleAccess.Membro, "/clientes"),
        web("metodos", "Métodos", "Métodos, aplicações e metas.", ModuleCategory.ClientesVendas, ModuleAccess.Membro, "/metodos"),
        web("atendimento", "Atendimento", "Chamados por setor, prioridade e andamento.", ModuleCategory.ClientesVendas, ModuleAccess.Membro, "/atendimento"),
        web("plataformas", "Desempenho por cliente", "Seguidores e posts de cada cliente em cada rede.", ModuleCategory.ClientesVendas, ModuleAccess.Membro, "/plataformas"),
        web("empresas-acessos", "Empresas e acessos", "Convites, redes sociais e acessos das empresas.", ModuleCategory.ClientesVendas, ModuleAccess.Membro, "/empresas"),

        web("producao", "Projetos e entregas", "Projetos, entregas e aprovações por etapa e prazo.", ModuleCategory.Operacao, ModuleAccess.Membro, "/producao"),
        web("agenda", "Agenda e prazos", "Prazos de tarefas comerciais e entregas.", ModuleCategory.Operacao, ModuleAccess.Membro, "/agenda"),
        web("youtube", "YouTube", "Inscritos e últimos vídeos de cada cliente.", ModuleCategory.Operacao, ModuleAccess.Membro, "/youtube"),
        web("instagram", "Instagram", "Seguidores e últimos posts de cada cliente.", ModuleCategory.Operacao, ModuleAccess.Membro, "/instagram"),

        web("aura", "A.U.R.A.", "Consulta operacional sobre os dados reais.", ModuleCategory.Inteligencia, ModuleAccess.Membro, "/aura"),
        web("inbox", "Caixa de entrada", "Prioridades de tarefas, entregas, leads e contratos.", ModuleCategory.Inteligencia, ModuleAccess.Membro, "/inbox"),
        web("sni", "Estratégia e narrativas", "Pilares, pautas e alertas estratégicos.", ModuleCategory.Inteligencia, ModuleAccess.Membro, "/sni"),

        native("biblioteca", "Biblioteca", "Materiais da operação e dos clientes (somente leitura; cadastro no site).", ModuleCategory.Conteudo, ModuleAccess.Membro, NativeTab.Biblioteca),

        web("gestao", "Visão da gestão", "Indicadores de gestão da operação.", ModuleCategory.Gestao, ModuleAccess.Admin, "/gestao"),
        web("financeiro", "Financeiro", "Contratos, despesas e custos da equipe.", ModuleCategory.Gestao, ModuleAccess.Admin, "/financeiro"),
        web("equipe", "Equipe", "Pessoas, áreas e níveis de acesso.", ModuleCategory.Gestao, ModuleAccess.Membro, "/equipe"),
        web("automacoes", "Sistemas e automações", "Conexões, endpoints e entregas dos fluxos.", ModuleCategory.Gestao, ModuleAccess.Membro, "/automacoes"),
        web("api-chaves", "API e chaves", "Chaves de acesso da API pública.", ModuleCategory.Gestao, ModuleAccess.Administrador, "/api-chaves"),

        web("configuracoes", "Configurações completas", "Equipe, permissões, alertas, integrações e segurança.", ModuleCategory.Configuracoes, ModuleAccess.Membro, "/configuracoes"),
    )

    val client: List<AppModule> = listOf(
        native("termometro", "Termômetro", "Avaliação publicada e resultados do período.", ModuleCategory.Inicio, ModuleAccess.Cliente, NativeTab.Termometro),
        native("leads", "Leads", "Leads captados do seu site e etapa de cada um.", ModuleCategory.ClientesVendas, ModuleAccess.Cliente, NativeTab.Leads),
        web("entregas", "Entregas", "Playbook de 30 dias e andamento do que está sendo entregue.", ModuleCategory.Operacao, ModuleAccess.Cliente, "/cliente/entregas"),
        web("pre-analise", "Pré-análise", "Evolução da pré-análise da sua empresa.", ModuleCategory.Inteligencia, ModuleAccess.Cliente, "/cliente/pre-analise", modulo = "preAnalise"),
        web("midias", "Mídias e relatórios", "Desempenho das suas redes sociais.", ModuleCategory.Conteudo, ModuleAccess.Cliente, "/cliente", modulo = "midias"),
        web("concorrentes", "Concorrentes", "Acompanhamento de concorrentes.", ModuleCategory.Inteligencia, ModuleAccess.Cliente, "/cliente", modulo = "concorrentes"),
        web("api-webhooks", "API e webhooks", "Integração do seu site com a Synergy.", ModuleCategory.Configuracoes, ModuleAccess.Cliente, "/cliente/configuracoes", modulo = "api"),
        native("biblioteca", "Biblioteca", "Materiais liberados para a sua empresa.", ModuleCategory.Conteudo, ModuleAccess.Cliente, NativeTab.Biblioteca),
        web("configuracoes", "Configurações da empresa", "Empresa, equipe, captação, alertas e segurança.", ModuleCategory.Configuracoes, ModuleAccess.Cliente, "/cliente/configuracoes"),
    )

    /** Módulos que o usuário pode ver, já filtrados por papel e (cliente) pelos módulos habilitados. */
    fun visibleFor(user: AuthenticatedUser, habilitados: Set<String>? = null): List<AppModule> {
        val base = if (user.role == UserRole.TEAM) team else client
        return base.filter { m ->
            m.access.allows(user) && (m.empresaModulo == null || habilitados == null || m.empresaModulo in habilitados)
        }
    }

    /** Termos e privacidade: páginas públicas do site (exigência de loja), iguais para todos. */
    val legal: List<Pair<String, String>> = listOf("Termos de uso" to "/termos", "Política de privacidade" to "/privacidade")
}
