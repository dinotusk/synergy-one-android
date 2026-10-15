package br.com.synergyone.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import br.com.synergyone.android.data.settings.ThemeMode
import br.com.synergyone.android.ui.navigation.SynergyNavGraph
import br.com.synergyone.android.ui.theme.SynergyOneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SynergyOneApplication).container

        setContent {
            val mode by container.themeSettings.mode.collectAsState()
            SynergyOneTheme(darkTheme = mode == ThemeMode.Dark) {
                SynergyNavGraph(container = container)
            }
        }
    }
}
