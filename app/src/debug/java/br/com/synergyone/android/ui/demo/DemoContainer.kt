package br.com.synergyone.android.ui.demo

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.*
import br.com.synergyone.android.data.auth.AuthRepository
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.EmpresaRef
import br.com.synergyone.android.data.auth.model.Papel
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.data.empresa.Empresa
import br.com.synergyone.android.data.empresa.EmpresaRepository
import br.com.synergyone.android.data.jornada.JornadaLocalizada
import br.com.synergyone.android.data.jornada.JornadaRepository
import br.com.synergyone.android.data.lead.LeadRepository
import br.com.synergyone.android.data.termometro.TermometroRepository
import br.com.synergyone.android.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DemoData {
    val clientUser = AuthenticatedUser(
        id = "demo-cliente", displayName = "Ana Souza", email = "ana@forlife.com.br",
        role = UserRole.CLIENT, papel = Papel.CLIENTE,
        empresas = listOf(EmpresaRef(3, "ForLife"), EmpresaRef(7, "Acme Odonto")),
    )

    fun teamUser(papel: String) = AuthenticatedUser(
        id = "demo-equipe", displayName = "Carlos Lima", email = "carlos@synergy.com.br",
        role = UserRole.TEAM, papel = Papel.fromWire(papel) ?: Papel.GESTAO, todasEmpresas = true,
    )

    private val resultados = ResultadosDto(
        periodo = PeriodoDto("2026-09-01", "2026-09-30"),
        totalLeads = 120, ganhos = 14, perdidos = 30, emAndamento = 76, conversao = 0.1167,
        diasAteVenda = DiasAteVendaDto(9.5, 7.0, 14),
        vendidoCentavos = 2_100_000, recebidoCentavos = 1_500_000,
        funil = listOf(
            FunilDto("captado", "Captado", 120), FunilDto("qualificado", "Qualificado", 80),
            FunilDto("contato", "Contato", 56), FunilDto("proposta", "Proposta", 31),
            FunilDto("negociacao", "Negociação", 19), FunilDto("ganho", "Ganho", 14),
        ),
        evolucao = listOf(
            EvolucaoDto("1", "01/09", 30, 3, 450_000, 300_000), EvolucaoDto("2", "08/09", 24, 2, 300_000, 200_000),
            EvolucaoDto("3", "15/09", 38, 5, 750_000, 500_000), EvolucaoDto("4", "22/09", 28, 4, 600_000, 500_000),
        ),
        cliques = CliquesDto(false, "Cliques ainda não são coletados: a fonte de dados está em definição."),
        aplicacao = AplicacaoResumoDto(
            "Método Outubro",
            listOf(
                MetaDto("ganhos", "Vendas ganhas", 12.0, 14.0),
                MetaDto("leads", "Leads captados", 150.0, 120.0),
                MetaDto("cliques", "Cliques no site", null, null, "Ainda não coletado."),
            ),
        ),
    )

    val publicado = TermometroClienteDto(
        empresa = EmpresaRefDto(3, "ForLife"),
        atual = AvaliacaoPublicadaDto(
            id = 18, periodoInicio = "2026-09-01", periodoFim = "2026-09-30", classificacao = "verde",
            justificativa = "Meta de vendas alcançada: 14 vendas contra a meta de 12, com boa conversão dos leads de Instagram.",
            feedback = "Ótimo mês! Próximo passo: reforçar o atendimento fora do horário comercial e testar uma oferta de retorno para leads perdidos.",
            publicadaEm = "2026-10-02T10:00:00.000Z", resultados = resultados,
        ),
        historico = listOf(
            AvaliacaoPublicadaDto(18, "2026-09-01", "2026-09-30", "verde"),
            AvaliacaoPublicadaDto(17, "2026-08-01", "2026-08-31", "amarelo"),
            AvaliacaoPublicadaDto(16, "2026-07-01", "2026-07-31", "azul"),
            AvaliacaoPublicadaDto(15, "2026-06-01", "2026-06-30", "lilas"),
            AvaliacaoPublicadaDto(14, "2026-05-01", "2026-05-31", "vermelho"),
        ),
    )

    val equipe = TermometroEquipeDto(
        empresas = listOf(
            EmpresaTermometroDto(3, "ForLife", UltimaAvaliacaoDto("verde", "publicada", "2026-09-30")),
            EmpresaTermometroDto(7, "Acme Odonto", UltimaAvaliacaoDto("amarelo", "rascunho", "2026-09-30")),
            EmpresaTermometroDto(9, "Studio Bela", null),
        ),
        empresaId = 3, periodo = PeriodoDto("2026-09-09", "2026-10-08"), resultados = resultados,
        aplicacoes = listOf(AplicacaoDto(2, "Método Outubro", "Captação local", "ativa", resultados.aplicacao!!.metas)),
        avaliacoes = listOf(
            AvaliacaoEquipeDto(19, 3, 2, "2026-10-01", "2026-10-31", "amarelo", "Abaixo da meta na metade do mês.", "Vamos reforçar o atendimento.", "Cliente demorou a aprovar a arte.", "rascunho", null),
            AvaliacaoEquipeDto(18, 3, 2, "2026-09-01", "2026-09-30", "verde", "Meta de vendas alcançada.", "Ótimo mês!", null, "publicada", "2026-10-02T10:00:00.000Z"),
        ),
        cliques = CliquesDto(false, "Cliques ainda não são coletados."),
        podeEditar = true, podePublicar = true,
    )

    val leads = listOf(
        LeadDto(1204, 3, "site", "instagram", "Carlos Lima", "carlos@exemplo.com", "+5511999990000", "São Paulo", "Quero saber valores e formas de pagamento.", "2026-10-07T14:02:11.000Z", "proposta", origem = "site", entradas = 2),
        LeadDto(1203, 3, "central", "legado", "Mariana Alves", null, "+5519988887777", "Campinas", null, "2026-10-06T09:10:00.000Z", "ganho", 150_000, "2026-10-10", origem = "Dra Bianca"),
        LeadDto(1202, 3, "site", "instagram", "João Pedro", null, null, null, "Vi o anúncio e gostei.", "2026-10-05T18:40:00.000Z", "qualificado"),
        LeadDto(1201, 3, "central", "legado", null, null, null, "Santos", null, "2026-10-04T11:00:00.000Z", "captado"),
        LeadDto(1200, 3, "site", "instagram", "Fernanda Rocha", null, null, "Sorocaba", "Tem horário na sexta?", "2026-10-03T16:20:00.000Z", "perdido"),
    )

    val empresas = listOf(
        Empresa(3, "ForLife", "forlife", "growth", "ativo", mapOf("leads" to true, "termometro" to true)),
        Empresa(7, "Acme Odonto", "acme", "start", "atencao", mapOf("leads" to true)),
        Empresa(9, "Studio Bela", "studio-bela", null, "inativo", emptyMap()),
    )
}

private suspend fun <T> demo(value: T): ApiResult<T> { delay(600); return ApiResult.Success(value) }

/** Jornada em memória: permite exercitar edição, notas e recebimentos sem tocar no servidor. */
class DemoJornada(private val canEdit: Boolean) : JornadaRepository {
    private val estado = mutableMapOf<Long, JornadaLeadDto>().apply {
        put(1204, JornadaLeadDto(1204, 3, "site", "instagram", "Carlos Lima", "site", "2026-10-07T14:02:11.000Z", "proposta", "proposta", "r1", 7, 150_000, "2026-10-08"))
        put(1203, JornadaLeadDto(1203, 3, "central", "legado", "Mariana Alves", "Dra Bianca", "2026-10-06T09:10:00.000Z", "ganho", "ganho", "r1", null, 150_000, "2026-10-05", 150_000, "2026-10-10", recebidoCentavos = 50_000))
        put(1202, JornadaLeadDto(1202, 3, "site", "instagram", "João Pedro", "site", "2026-10-05T18:40:00.000Z", "qualificado", "qualificado"))
    }
    private val notas = mutableMapOf<Long, MutableList<NotaDto>>(1204L to mutableListOf(NotaDto(9, "Pediu desconto à vista.", "Carlos Lima", "2026-10-09T11:00:00.000Z")))
    private val recebimentos = mutableMapOf<Long, MutableList<RecebimentoDto>>(1203L to mutableListOf(RecebimentoDto(4, 50_000, "2026-10-12")))
    private val historico = mutableMapOf<Long, MutableList<HistoricoItemDto>>(
        1204L to mutableListOf(HistoricoItemDto(2, "contato", "proposta", "Carlos Lima", "2026-10-08T10:00:00.000Z"), HistoricoItemDto(1, "captado", "contato", "Carlos Lima", "2026-10-07T16:00:00.000Z")),
    )
    private val apoio get() = JornadaPage(
        podeEditar = canEdit,
        empresas = listOf(EmpresaRefDto(3, "ForLife")),
        campanhas = listOf(CampanhaDto(7, 3, "Outubro Rosa", "instagram"), CampanhaDto(8, 3, "Retorno de clientes", "google_ads")),
        responsaveis = listOf(ResponsavelDto("r1", "Carlos Lima"), ResponsavelDto("r2", "Ana Souza")),
    )

    override suspend fun estadoDoLead(leadId: Long, empresaId: Long, capturadoEm: String?): ApiResult<JornadaLocalizada?> {
        delay(400); return ApiResult.Success(estado[leadId]?.let { JornadaLocalizada(it, apoio) })
    }
    override suspend fun detalhe(leadId: Long): ApiResult<JornadaDetalheDto> {
        delay(300)
        return ApiResult.Success(JornadaDetalheDto(historico[leadId].orEmpty(), notas[leadId].orEmpty(), recebimentos[leadId].orEmpty()))
    }
    override suspend fun salvar(leadId: Long, estado: br.com.synergyone.android.data.api.JornadaEstadoRequest): ApiResult<Unit> {
        delay(500)
        val atual = this.estado[leadId] ?: return ApiResult.Error("Lead não encontrado.")
        fun cent(v: String?) = v?.toBigDecimal()?.movePointRight(2)?.toLong()
        this.estado[leadId] = atual.copy(
            etapa = estado.etapa, responsavelId = estado.responsavelId, campanhaId = estado.campanhaId,
            propostaCentavos = cent(estado.propostaValor), propostaEnviadaEm = estado.propostaEnviadaEm,
            vendaCentavos = cent(estado.vendaValor), vendaFechadaEm = estado.vendaFechadaEm,
            motivoPerda = estado.motivoPerda, perdidoEm = estado.perdidoEm,
        )
        if (atual.etapa != estado.etapa) historico.getOrPut(leadId) { mutableListOf() }.add(0, HistoricoItemDto(99, atual.etapa, estado.etapa, "Você", "2026-10-15T14:00:00.000Z"))
        return ApiResult.Success(Unit)
    }
    override suspend fun adicionarNota(leadId: Long, texto: String): ApiResult<Unit> {
        delay(300); notas.getOrPut(leadId) { mutableListOf() }.add(0, NotaDto(100, texto, "Você", "2026-10-15T14:00:00.000Z")); return ApiResult.Success(Unit)
    }
    override suspend fun registrarRecebimento(leadId: Long, valor: String, recebidoEm: String): ApiResult<Unit> {
        delay(300)
        if (estado[leadId]?.etapa != "ganho") return ApiResult.Error("Só é possível receber depois da venda.", code = "conflito")
        recebimentos.getOrPut(leadId) { mutableListOf() }.add(RecebimentoDto(101, valor.toBigDecimal().movePointRight(2).toLong(), recebidoEm))
        return ApiResult.Success(Unit)
    }
}

class DemoContainer(context: android.content.Context, private val demoUser: AuthenticatedUser, private val vazio: Boolean = false, private val semAvaliacao: Boolean = false, private val acimaDoLimite: Boolean = false) : AppContainer {
    override val themeSettings = br.com.synergyone.android.data.settings.ThemeSettings(context)
    override val jornadaRepository: JornadaRepository = DemoJornada(demoUser.canWrite)
    override val authRepository = object : AuthRepository {
        override val currentUser: StateFlow<AuthenticatedUser?> = MutableStateFlow(demoUser)
        override suspend fun login(email: String, password: String): ApiResult<AuthenticatedUser> = ApiResult.Error("demo")
        override suspend fun restoreSession(): AuthenticatedUser? = demoUser
        override suspend fun logout() = Unit
        override suspend fun accessToken(rejected: String?): String? = null
    }
    override val empresaRepository = object : EmpresaRepository {
        override suspend fun listar() = demo(DemoData.empresas)
    }
    private val avaliacoes = DemoData.equipe.avaliacoes.toMutableList()
    override val termometroRepository = object : TermometroRepository {
        override suspend fun salvarAvaliacao(id: Long?, request: AvaliacaoRequest): ApiResult<Long> {
            delay(500)
            val existente = id?.let { i -> avaliacoes.firstOrNull { it.id == i } }
            if (existente?.status == "publicada") return ApiResult.Error("Avaliação publicada não pode ser editada.", code = "conflito")
            val nova = AvaliacaoEquipeDto(
                id ?: (avaliacoes.maxOf { it.id } + 1), request.empresaId, request.aplicacaoId, request.periodoInicio, request.periodoFim,
                request.classificacao, request.justificativa, request.feedback, request.notaInterna, "rascunho", null, "2026-10-15T14:00:00.000Z",
            )
            if (existente != null) avaliacoes[avaliacoes.indexOf(existente)] = nova else avaliacoes.add(0, nova)
            return ApiResult.Success(nova.id)
        }
        override suspend fun publicar(id: Long): ApiResult<Unit> {
            delay(500)
            val i = avaliacoes.indexOfFirst { it.id == id }
            if (i < 0) return ApiResult.Error("Avaliação não encontrada.", code = "nao_encontrado")
            avaliacoes[i] = avaliacoes[i].copy(status = "publicada", publicadaEm = "2026-10-15T14:05:00.000Z")
            return ApiResult.Success(Unit)
        }
        override suspend fun publicado(empresaId: Long?) = demo(if (vazio || semAvaliacao) TermometroClienteDto(EmpresaRefDto(3, "ForLife")) else DemoData.publicado)
        override suspend fun painelEquipe(empresaId: Long?, desde: String?, ate: String?) = demo(if (vazio) DemoData.equipe.copy(avaliacoes = emptyList(), resultados = ResultadosDto(), aplicacoes = emptyList()) else DemoData.equipe.copy(avaliacoes = avaliacoes.toList(), podeEditar = demoUser.canWrite, podePublicar = demoUser.isAdminOrManager))
    }
    override val leadRepository = object : LeadRepository {
        override fun cached(id: Long): LeadDto? = DemoData.leads.firstOrNull { it.id == id }
        override suspend fun pagina(empresaId: Long?, etapa: String?, cursor: String?, desde: String?, limite: Int) =
            demo(LeadsPage(DemoData.leads.filter { !vazio }.filter { desde == null || (it.capturadoEm ?: "").take(10) >= desde }.filter { etapa == null || it.etapa == etapa }, DemoData.leads.size, if (acimaDoLimite) "c" else null).let { if (acimaDoLimite) it.copy(total = 5200) else it })
    }
}
