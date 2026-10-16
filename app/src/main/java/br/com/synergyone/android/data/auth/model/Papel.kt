package br.com.synergyone.android.data.auth.model

/** Papéis reais do backend (`synergy_one.usuarios.papel`). Valor desconhecido = sem acesso. */
enum class Papel(val wire: String) {
    ADMIN("admin"),
    GESTAO("gestao"),
    OPERACAO("operacao"),
    LEITOR("leitor"),
    CLIENTE("cliente");

    companion object {
        fun fromWire(value: String?): Papel? = entries.firstOrNull { it.wire == value }
    }
}
