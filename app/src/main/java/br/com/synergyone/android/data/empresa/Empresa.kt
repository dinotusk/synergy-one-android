package br.com.synergyone.android.data.empresa

/** Empresa visível ao usuário (`synergy_one.empresa_crm`); quem vê o quê é decidido pela RLS. */
data class Empresa(
    val id: Long,
    val nome: String,
    val slug: String?,
    val plano: String?,
    val status: String?,
    /** Módulos do portal do cliente habilitados para esta empresa (ex.: leads, midias). */
    val modulos: Map<String, Boolean>,
) {
    val modulosAtivos: List<String> get() = modulos.filterValues { it }.keys.sorted()
}
