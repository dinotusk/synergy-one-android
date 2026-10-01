package br.com.synergyone.android

import android.app.Application
import br.com.synergyone.android.di.AppContainer
import br.com.synergyone.android.di.DefaultAppContainer

class SynergyOneApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
