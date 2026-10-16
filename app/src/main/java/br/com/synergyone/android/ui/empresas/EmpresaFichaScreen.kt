package br.com.synergyone.android.ui.empresas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.api.LeadDto
import br.com.synergyone.android.data.api.ResultadosDto
import br.com.synergyone.android.data.auth.model.AuthenticatedUser
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.data.empresa.Empresa
import br.com.synergyone.android.di.AppContainer
import br.com.synergyone.android.ui.common.LoadState
import br.com.synergyone.android.ui.common.LoadStateContent
import br.com.synergyone.android.ui.common.SkeletonBlock
import br.com.synergyone.android.ui.common.etapaLabel
import br.com.synergyone.android.ui.common.formatDate
import br.com.synergyone.android.ui.common.formatPercent
import br.com.synergyone.android.ui.common.formatPeriod
import br.com.synergyone.android.ui.common.moduloLabel
import br.com.synergyone.android.ui.common.origemLabel
import br.com.synergyone.android.ui.common.PullRefresh
import br.com.synergyone.android.ui.common.rememberLoader
import br.com.synergyone.android.ui.components.AppTopBar
import br.com.synergyone.android.ui.components.BadgeTone
import br.com.synergyone.android.ui.components.EmptyDashboardState
import br.com.synergyone.android.ui.components.SectionTitle
import br.com.synergyone.android.ui.components.StatusBadge
import br.com.synergyone.android.ui.components.SynergyCard
import br.com.synergyone.android.ui.modules.ModuleRow
import br.com.synergyone.android.ui.navigation.AppNavigator
import br.com.synergyone.android.ui.termometro.Classificacao
import br.com.synergyone.android.ui.termometro.ClassificationBadge
import br.com.synergyone.android.ui.termometro.ClassificationScale

/** Dados mínimos do Termômetro para o resumo da ficha (equipe e cliente têm rotas diferentes). */
private data class ResumoTermometro(
    val classificacao: Classificacao,
    val periodo: String?,
    val justificativa: String?,
    val resultados: ResultadosDto?,
    val resultadosRotulo: String,
)

/**
 * Ficha da empresa: ponto de entrada para os resultados, os leads e os módulos autorizados dela.
 * Cada seção carrega sozinha: se uma falhar, as outras continuam úteis.
 */
@Composable
fun EmpresaFichaScreen(
    empresaId: Long,
    user: AuthenticatedUser,
    container: AppContainer,
    nav: AppNavigator,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val empresaLoader = rememberLoader("ficha-empresa") { container.empresaRepository.listar() }
    val empresas by empresaLoader.state.collectAsState()
    val isTeam = user.role == UserRole.TEAM

    val resumoLoader = rememberLoader("ficha-termometro-$empresaId-$isTeam") {
        if (isTeam) {
            when (val r = container.termometroRepository.painelEquipe(empresaId, null, null)) {
                is br.com.synergyone.android.core.result.ApiResult.Error -> r
                is br.com.synergyone.android.core.result.ApiResult.Success -> {
                    val pub = r.data.avaliacoes.firstOrNull { it.status == "publicada" }
                    br.com.synergyone.android.core.result.ApiResult.Success(
                        ResumoTermometro(
                            Classificacao.fromWire(pub?.classificacao),
                            pub?.let { formatPeriod(it.periodoInicio, it.periodoFim) },
                            pub?.justificativa,
                            r.data.resultados,
                            "Últimos 30 dias, ao vivo",
                        ),
                    )
                }
            }
        } else {
            when (val r = container.termometroRepository.publicado(empresaId)) {
                is br.com.synergyone.android.core.result.ApiResult.Error -> r
                is br.com.synergyone.android.core.result.ApiResult.Success -> {
                    val a = r.data.atual
                    br.com.synergyone.android.core.result.ApiResult.Success(
                        ResumoTermometro(
                            Classificacao.fromWire(a?.classificacao),
                            a?.let { formatPeriod(it.periodoInicio, it.periodoFim) },
                            a?.justificativa,
                            a?.resultados,
                            a?.let { "Período avaliado: " + formatPeriod(it.periodoInicio, it.periodoFim) } ?: "",
                        ),
                    )
                }
            }
        }
    }
    val resumo by resumoLoader.state.collectAsState()

    val leadsLoader = rememberLoader("ficha-leads-$empresaId") { container.leadRepository.pagina(empresaId = empresaId) }
    val leads by leadsLoader.state.collectAsState()

    val empresa = (empresas as? LoadState.Loaded)?.data?.firstOrNull { it.id == empresaId }
    val nome = empresa?.nome ?: user.empresas.firstOrNull { it.id == empresaId }?.nome ?: "Empresa"

    PullRefresh(listOf(empresaLoader, resumoLoader, leadsLoader), modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppTopBar(title = nome, userName = user.displayName, onAccount = nav::openPerfil, onBack = nav::back)
            empresa?.let { Cabecalho(it) }

            // Termômetro + resultados
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Termômetro e resultados", eyebrow = "Avaliação")
                LoadStateContent(resumo, onRetry = resumoLoader::reload, onSignIn = onSignIn) { r ->
                    SynergyCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ClassificationBadge(r.classificacao)
                            ClassificationScale(r.classificacao)
                            r.periodo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            r.justificativa?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3) }
                            if (r.classificacao == Classificacao.NaoAvaliado) {
                                Text("Ainda não há avaliação publicada.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            r.resultados?.takeIf { it.totalLeads > 0 || it.ganhos > 0 }?.let { ResumoNumeros(it, r.resultadosRotulo) }
                            OutlinedButton(onClick = { nav.openTermometroDaEmpresa(empresaId) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Abrir Termômetro completo")
                            }
                        }
                    }
                }
            }

            // Leads
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Leads", eyebrow = "Captação")
                LoadStateContent(leads, onRetry = leadsLoader::reload, onSignIn = onSignIn) { page ->
                    if (page.leads.isEmpty()) {
                        EmptyDashboardState("Nenhum lead ainda", "Quando um lead for captado para esta empresa, ele aparece aqui.")
                    } else {
                        SynergyCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                page.leads.take(5).forEachIndexed { i, lead ->
                                    LeadLinha(lead, onClick = if (isTeam) ({ nav.openLead(lead.id) }) else null)
                                    if (i != minOf(page.leads.size, 5) - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                        OutlinedButton(onClick = { nav.openLeadsDaEmpresa(empresaId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ver todos os leads (${page.total})")
                        }
                    }
                }
            }

            // Módulos autorizados
            if (empresa != null) ModulosAutorizados(empresa, isTeam, nav)
            else if (empresas is LoadState.Loading) SkeletonBlock(96.dp)
        }
    }
}

@Composable
private fun Cabecalho(e: Empresa) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        e.status?.let { StatusBadge(statusLabel(it).replaceFirstChar { c -> c.uppercase() }, statusTone(it)) }
        e.plano?.let { StatusBadge("Plano $it", BadgeTone.Neutral) }
    }
}

@Composable
private fun ResumoNumeros(r: ResultadosDto, rotulo: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (rotulo.isNotBlank()) Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Numero("Leads", r.totalLeads.toString(), Modifier.weight(1f))
            Numero("Vendas", r.ganhos.toString(), Modifier.weight(1f))
            Numero("Conversão", formatPercent(r.conversao), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Numero(rotulo: String, valor: String, modifier: Modifier) {
    Column(modifier) {
        Text(valor, style = MaterialTheme.typography.titleLarge)
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LeadLinha(lead: LeadDto, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClickLabel = "Abrir lead", onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(lead.nome?.takeIf { it.isNotBlank() } ?: "Lead sem nome", style = MaterialTheme.typography.titleSmall, maxLines = 1)
            val sub = listOfNotNull(origemLabel(lead.origem, lead.canal), formatDate(lead.capturadoEm).takeIf { it != "—" }).joinToString(" • ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusBadge(etapaLabel(lead.etapa), br.com.synergyone.android.ui.leads.etapaTone(lead.etapa))
    }
}

@Composable
private fun ModulosAutorizados(empresa: Empresa, isTeam: Boolean, nav: AppNavigator) {
    val ativos = empresa.modulosAtivos
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Módulos", eyebrow = "Autorizados para esta empresa")
        SynergyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                var first = true
                if (isTeam) {
                    ModuleRow("Cliente 360 completo", "Projetos, entregas, prazos e histórico no site.", external = true, onClick = { nav.openWeb("/clientes/${empresa.id}") })
                    first = false
                }
                ativos.forEach { chave ->
                    if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    first = false
                    when (chave) {
                        "leads" -> ModuleRow(moduloLabel(chave), "Lista de leads desta empresa.", external = false, onClick = { nav.openLeadsDaEmpresa(empresa.id) })
                        "termometro" -> ModuleRow(moduloLabel(chave), "Avaliação e resultados.", external = false, onClick = { nav.openTermometroDaEmpresa(empresa.id) })
                        else -> ModuleRow(moduloLabel(chave), null, external = true, onClick = { nav.openWeb(if (isTeam) "/clientes/${empresa.id}" else "/cliente") })
                    }
                }
                if (ativos.isEmpty() && !isTeam) {
                    Text("Nenhum módulo adicional habilitado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
