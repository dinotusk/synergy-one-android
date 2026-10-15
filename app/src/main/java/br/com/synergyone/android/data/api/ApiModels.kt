package br.com.synergyone.android.data.api

import kotlinx.serialization.Serializable

// Modelos de resposta da API dos apps (`/api/app/v1`). Tolerantes: campos novos são ignorados e
// ausentes viram padrão, para o app não quebrar quando o backend evoluir.

@Serializable data class EmpresaRefDto(val id: Long, val nome: String)

@Serializable
data class MeDto(
    val id: String,
    val nome: String? = null,
    val email: String? = null,
    val tipo: String? = null,
    val papel: String? = null,
    val todasEmpresas: Boolean = false,
    val empresas: List<EmpresaRefDto> = emptyList(),
    val semEmpresa: Boolean = false,
)

@Serializable data class EmpresasResponse(val empresas: List<EmpresaDto> = emptyList())

@Serializable
data class EmpresaDto(
    val id: Long,
    val nome: String,
    val slug: String? = null,
    val plano: String? = null,
    val status: String? = null,
    val modulos: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class LeadDto(
    val id: Long,
    val empresaId: Long? = null,
    val fonte: String? = null,
    val canal: String? = null,
    val nome: String? = null,
    val email: String? = null,
    val telefone: String? = null,
    val cidade: String? = null,
    val mensagem: String? = null,
    val capturadoEm: String? = null,
    val etapa: String = "captado",
    val vendaCentavos: Long? = null,
    val vendaFechadaEm: String? = null,
    /** Nome real da origem (ex.: o cliente de onde veio); `null` quando desconhecida. */
    val origem: String? = null,
    /** Quantas vezes a mesma pessoa entrou na mesma empresa (1 = só esta). */
    val entradas: Int = 1,
)

@Serializable
data class LeadsPage(
    val leads: List<LeadDto> = emptyList(),
    val total: Int = 0,
    val proximoCursor: String? = null,
)

@Serializable data class PeriodoDto(val inicio: String? = null, val fim: String? = null)
@Serializable data class DiasAteVendaDto(val media: Double? = null, val mediana: Double? = null, val amostras: Int = 0)
@Serializable data class FunilDto(val etapa: String, val rotulo: String? = null, val total: Int = 0)

@Serializable
data class EvolucaoDto(
    val chave: String = "",
    val rotulo: String = "",
    val captados: Int = 0,
    val ganhos: Int = 0,
    val vendidoCentavos: Long = 0,
    val recebidoCentavos: Long = 0,
)

@Serializable data class CliquesDto(val disponivel: Boolean = false, val pendencia: String? = null)

@Serializable
data class MetaDto(
    val indicador: String = "",
    val rotulo: String = "",
    val alvo: Double? = null,
    val realizado: Double? = null,
    val pendencia: String? = null,
)

@Serializable data class AplicacaoResumoDto(val nome: String = "", val metas: List<MetaDto> = emptyList())

@Serializable
data class ResultadosDto(
    val periodo: PeriodoDto? = null,
    val totalLeads: Int = 0,
    val ganhos: Int = 0,
    val perdidos: Int = 0,
    val emAndamento: Int? = null,
    val conversao: Double? = null,
    val diasAteVenda: DiasAteVendaDto? = null,
    val vendidoCentavos: Long = 0,
    val recebidoCentavos: Long = 0,
    val funil: List<FunilDto> = emptyList(),
    val evolucao: List<EvolucaoDto> = emptyList(),
    val cliques: CliquesDto? = null,
    val aplicacao: AplicacaoResumoDto? = null,
)

@Serializable
data class AvaliacaoPublicadaDto(
    val id: Long,
    val periodoInicio: String? = null,
    val periodoFim: String? = null,
    val classificacao: String? = null,
    val justificativa: String? = null,
    val feedback: String? = null,
    val publicadaEm: String? = null,
    val resultados: ResultadosDto? = null,
)

/** `GET /termometro`: só o que foi publicado (o que o cliente vê no portal). */
@Serializable
data class TermometroClienteDto(
    val empresa: EmpresaRefDto? = null,
    val atual: AvaliacaoPublicadaDto? = null,
    val historico: List<AvaliacaoPublicadaDto> = emptyList(),
)

@Serializable
data class UltimaAvaliacaoDto(val classificacao: String? = null, val status: String? = null, val periodoFim: String? = null)

@Serializable
data class EmpresaTermometroDto(val id: Long, val nome: String, val ultima: UltimaAvaliacaoDto? = null)

@Serializable
data class AplicacaoDto(
    val id: Long,
    val nome: String = "",
    val metodoNome: String? = null,
    val status: String? = null,
    val metas: List<MetaDto> = emptyList(),
    val semCampanhas: Boolean = false,
)

@Serializable
data class AvaliacaoEquipeDto(
    val id: Long,
    val empresaId: Long? = null,
    val aplicacaoId: Long? = null,
    val periodoInicio: String? = null,
    val periodoFim: String? = null,
    val classificacao: String? = null,
    val justificativa: String? = null,
    val feedback: String? = null,
    val notaInterna: String? = null,
    val status: String = "rascunho",
    val publicadaEm: String? = null,
    val atualizadoEm: String? = null,
)

/** `GET /equipe/termometro`: painel da equipe (ao vivo + rascunhos + notas internas). */
@Serializable
data class TermometroEquipeDto(
    val empresas: List<EmpresaTermometroDto> = emptyList(),
    val empresaId: Long? = null,
    val periodo: PeriodoDto? = null,
    val resultados: ResultadosDto? = null,
    val truncado: Boolean = false,
    val aplicacoes: List<AplicacaoDto> = emptyList(),
    val avaliacoes: List<AvaliacaoEquipeDto> = emptyList(),
    val cliques: CliquesDto? = null,
    val podeEditar: Boolean = false,
    val podePublicar: Boolean = false,
)

// ── Jornada do lead (`/equipe/jornada`) ─────────────────────────────────────────────────────────

@Serializable data class ResponsavelDto(val id: String, val nome: String = "")

@Serializable
data class CampanhaDto(
    val id: Long,
    val empresaId: Long? = null,
    val nome: String = "",
    val canal: String? = null,
    val utmCampaign: String? = null,
    val aplicacaoId: Long? = null,
)

@Serializable
data class JornadaLeadDto(
    val id: Long,
    val empresaId: Long? = null,
    val fonte: String? = null,
    val canal: String? = null,
    val nome: String? = null,
    val origem: String? = null,
    val capturadoEm: String? = null,
    val etapa: String = "captado",
    val etapaMaxima: String? = null,
    val responsavelId: String? = null,
    val campanhaId: Long? = null,
    val propostaCentavos: Long? = null,
    val propostaEnviadaEm: String? = null,
    val vendaCentavos: Long? = null,
    val vendaFechadaEm: String? = null,
    val motivoPerda: String? = null,
    val perdidoEm: String? = null,
    val clienteEmpresaId: Long? = null,
    val recebidoCentavos: Long? = null,
)

@Serializable
data class JornadaPage(
    val leads: List<JornadaLeadDto> = emptyList(),
    val total: Int = 0,
    val proximoCursor: String? = null,
    val truncado: Boolean = false,
    val podeEditar: Boolean = false,
    val empresas: List<EmpresaRefDto> = emptyList(),
    val campanhas: List<CampanhaDto> = emptyList(),
    val responsaveis: List<ResponsavelDto> = emptyList(),
)

@Serializable
data class HistoricoItemDto(
    val id: Long = 0,
    val etapaDe: String? = null,
    val etapaPara: String? = null,
    val autor: String? = null,
    val em: String? = null,
)

@Serializable data class NotaDto(val id: Long = 0, val texto: String = "", val autor: String? = null, val criadaEm: String? = null)
@Serializable data class RecebimentoDto(val id: Long = 0, val centavos: Long = 0, val recebidoEm: String? = null)

@Serializable
data class JornadaDetalheDto(
    val historico: List<HistoricoItemDto> = emptyList(),
    val notas: List<NotaDto> = emptyList(),
    val recebimentos: List<RecebimentoDto> = emptyList(),
)

/** Corpo do `PUT /equipe/jornada/{id}`: estado **completo** (`null` nos campos vazios). */
@Serializable
data class JornadaEstadoRequest(
    val etapa: String,
    val responsavelId: String?,
    val campanhaId: Long?,
    val propostaValor: String?,
    val propostaEnviadaEm: String?,
    val vendaValor: String?,
    val vendaFechadaEm: String?,
    val motivoPerda: String?,
    val perdidoEm: String?,
    val clienteEmpresaId: Long?,
)

@Serializable data class NotaRequest(val texto: String)
@Serializable data class RecebimentoRequest(val valor: String, val recebidoEm: String)

/** Corpo do `POST`/`PUT /equipe/termometro/avaliacoes` (estado completo). */
@Serializable
data class AvaliacaoRequest(
    val empresaId: Long,
    val aplicacaoId: Long?,
    val periodoInicio: String,
    val periodoFim: String,
    val classificacao: String,
    val justificativa: String,
    val feedback: String,
    val notaInterna: String?,
)

@Serializable data class IdResponse(val id: Long = 0)
