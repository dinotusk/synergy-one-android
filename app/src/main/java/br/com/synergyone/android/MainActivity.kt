package br.com.synergyone.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.synergyone.android.ui.navigation.SynergyNavGraph
import br.com.synergyone.android.ui.theme.SynergyOneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authRepository = (application as SynergyOneApplication).container.authRepository

        setContent {
            SynergyOneTheme {
                SynergyNavGraph(authRepository = authRepository)
            }
        }
    }
}
