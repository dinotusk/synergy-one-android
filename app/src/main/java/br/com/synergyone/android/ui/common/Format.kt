package br.com.synergyone.android.ui.common

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBr = Locale("pt", "BR")
private val fullDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val shortDate = DateTimeFormatter.ofPattern("dd/MM")

/** A API manda dinheiro em centavos inteiros. */
fun formatMoney(centavos: Long): String = NumberFormat.getCurrencyInstance(ptBr).format(centavos / 100.0)

/** `conversao` chega como fração (0,1167 = 11,7%); `null` = sem dados. */
fun formatPercent(fraction: Double?): String =
    if (fraction == null) "—" else NumberFormat.getPercentInstance(ptBr).apply { maximumFractionDigits = 1 }.format(fraction)

fun formatNumber(value: Double?): String =
    if (value == null) "—" else NumberFormat.getNumberInstance(ptBr).apply { maximumFractionDigits = 1 }.format(value)

/** Aceita `AAAA-MM-DD` ou um instante ISO 8601 (usa só a parte da data). */
private fun parseDate(iso: String?): LocalDate? =
    runCatching { LocalDate.parse(iso!!.take(10)) }.getOrNull()

fun formatDate(iso: String?): String = parseDate(iso)?.format(fullDate) ?: "—"

fun formatPeriod(inicio: String?, fim: String?): String {
    val a = parseDate(inicio) ?: return "Período não informado"
    val b = parseDate(fim) ?: return a.format(fullDate)
    return if (a.year == b.year) "${a.format(shortDate)} – ${b.format(fullDate)}" else "${a.format(fullDate)} – ${b.format(fullDate)}"
}

/** Versão curta para botões: `01/09 – 30/09` (o ano só aparece se os extremos forem de anos diferentes). */
fun formatPeriodShort(inicio: String?, fim: String?): String {
    val a = parseDate(inicio) ?: return "Sem período"
    val b = parseDate(fim) ?: return a.format(fullDate)
    return if (a.year == b.year) "${a.format(shortDate)} – ${b.format(shortDate)}" else "${a.format(fullDate)} – ${b.format(fullDate)}"
}

fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

val etapas = listOf("captado", "qualificado", "contato", "proposta", "negociacao", "ganho", "perdido")

fun etapaLabel(etapa: String?): String = when (etapa) {
    "captado" -> "Captado"
    "qualificado" -> "Qualificado"
    "contato" -> "Contato"
    "proposta" -> "Proposta"
    "negociacao" -> "Negociação"
    "ganho" -> "Ganho"
    "perdido" -> "Perdido"
    else -> etapa?.replaceFirstChar { it.uppercase() } ?: "—"
}

fun canalLabel(canal: String?): String? = canal?.takeIf { it.isNotBlank() }
    ?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }

fun papelLabel(wire: String): String = when (wire) {
    "admin" -> "Administrador"
    "gestao" -> "Gestão"
    "operacao" -> "Operação"
    "leitor" -> "Leitor"
    "cliente" -> "Cliente"
    else -> wire
}

/**
 * Nome de exibição dos módulos de uma empresa (as chaves vêm do backend em camelCase).
 * Os mesmos rótulos do site (`modulosCliente`); chave desconhecida vira texto legível, nunca a chave crua.
 */
fun moduloLabel(chave: String): String = when (chave) {
    "leads" -> "Leads e funil"
    "preAnalise" -> "Pré-análise"
    "midias" -> "Mídias e relatórios"
    "concorrentes" -> "Concorrentes"
    "api" -> "API e webhooks"
    "termometro" -> "Termômetro"
    else -> chave.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ')
        .lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Origem a exibir para um lead: a origem real quando conhecida; senão o canal. "legado" **não** é
 * canal de aquisição (é só a marca dos leads trazidos da base antiga): nunca aparece como origem.
 */
fun origemLabel(origem: String?, canal: String?): String? {
    origem?.trim()?.takeIf { it.isNotEmpty() && !it.equals("legado", ignoreCase = true) }?.let { return it }
    return canalLabel(canal?.takeUnless { it.equals("legado", ignoreCase = true) })
}
