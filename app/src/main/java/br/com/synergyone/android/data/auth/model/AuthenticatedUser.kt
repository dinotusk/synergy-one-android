package br.com.synergyone.android.data.auth.model

/** Representa o usuário autenticado. Campos sujeitos a ajuste conforme contrato real do backend. */
data class AuthenticatedUser(
    val id: String,
    val displayName: String,
    val role: UserRole,
)
