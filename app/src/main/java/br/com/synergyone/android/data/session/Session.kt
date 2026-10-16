package br.com.synergyone.android.data.session

/** Sessão Supabase persistida no aparelho (sempre cifrada em repouso). */
data class Session(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    /** Epoch em segundos. */
    val expiresAt: Long,
) {
    // Evita vazar tokens em logs/toString.
    override fun toString() = "Session(userId=$userId, expiresAt=$expiresAt)"
}

interface SessionStore {
    fun load(): Session?
    fun save(session: Session)
    fun clear()
}

class InMemorySessionStore(private var session: Session? = null) : SessionStore {
    override fun load() = session
    override fun save(session: Session) { this.session = session }
    override fun clear() { session = null }
}
