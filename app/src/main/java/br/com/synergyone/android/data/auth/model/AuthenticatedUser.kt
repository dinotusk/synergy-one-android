package br.com.synergyone.android.data.auth.model

data class EmpresaRef(val id: Long, val nome: String)

/**
 * Resposta de `GET /api/app/v1/me`. As capacidades abaixo servem só para mostrar/esconder UI;
 * a autorização verdadeira é do servidor (RLS).
 */
data class AuthenticatedUser(
    val id: String,
    val displayName: String,
    val email: String,
    val role: UserRole,
    val papel: Papel,
    /** Cliente: as empresas dele. Equipe: vazio (veja [todasEmpresas]). */
    val empresas: List<EmpresaRef> = emptyList(),
    val todasEmpresas: Boolean = false,
    /** Cliente logado, mas sem empresa vinculada: não há dados para ver. */
    val semEmpresa: Boolean = false,
) {
    val canWrite: Boolean get() = role == UserRole.TEAM && papel != Papel.LEITOR
    val isAdminOrManager: Boolean get() = role == UserRole.TEAM && (papel == Papel.ADMIN || papel == Papel.GESTAO)
    val isAdmin: Boolean get() = role == UserRole.TEAM && papel == Papel.ADMIN
}
