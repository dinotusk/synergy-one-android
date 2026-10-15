package br.com.synergyone.android.ui.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.navigation.NavRoutes
import br.com.synergyone.android.ui.navigation.SynergyNavGraph
import br.com.synergyone.android.ui.theme.SynergyOneTheme

/**
 * Pré-visualização das telas reais com dados fictícios (só debug). Não passa pelo login.
 *   adb shell am start -n br.com.synergyone.android/.ui.demo.DemoActivity --es view team|client [--es role leitor|gestao|admin]
 */
class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isClient = intent.getStringExtra("view") == "client"
        val user = if (isClient) DemoData.clientUser else DemoData.teamUser(intent.getStringExtra("role") ?: "gestao")
        val container = DemoContainer(this, user, vazio = intent.getStringExtra("state") == "empty", semAvaliacao = intent.getStringExtra("state") == "noeval")
        setContent {
            val mode by container.themeSettings.mode.collectAsState()
            SynergyOneTheme(darkTheme = mode == br.com.synergyone.android.data.settings.ThemeMode.Dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            "DEMONSTRAÇÃO • dados fictícios",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 4.dp),
                        )
                        Column(Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)) {
                            SynergyNavGraph(container, startDestination = if (isClient) NavRoutes.CLIENT_HOME else NavRoutes.TEAM_HOME)
                        }
                    }
                }
            }
        }
    }
}
