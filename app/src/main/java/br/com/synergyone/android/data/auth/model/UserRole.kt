package br.com.synergyone.android.data.auth.model

/**
 * Perfis de acesso suportados pelo app. O valor real virá do backend após a autenticação
 * (ver contrato proposto em docs/API_CONTRACTS.md) — ainda não confirmado.
 */
enum class UserRole {
    TEAM,
    CLIENT,
}
