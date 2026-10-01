package br.com.synergyone.android.di

import br.com.synergyone.android.data.auth.AuthRepository
import br.com.synergyone.android.data.auth.PendingAuthRepository

/** Pequeno contêiner manual de dependências (sem framework de DI por enquanto). */
interface AppContainer {
    val authRepository: AuthRepository
}

class DefaultAppContainer : AppContainer {
    override val authRepository: AuthRepository by lazy { PendingAuthRepository() }
}
