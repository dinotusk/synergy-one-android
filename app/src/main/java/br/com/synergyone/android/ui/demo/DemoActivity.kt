package br.com.synergyone.android.ui.demo

import android.os.Bundle
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.client.ClientDemoScreen
import br.com.synergyone.android.ui.team.TeamDemoScreen
import br.com.synergyone.android.ui.theme.SynergyOneTheme

/**
 * Destino interno para validação visual. Não integra o fluxo de login e é encerrado em builds
 * que não sejam debug, impedindo a exposição de dados demonstrativos ao usuário final. O
 * componente é exportado somente para permitir a captura externa de QA em builds de debug.
 */
class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            finish()
            return
        }
        val isClient = intent.getStringExtra(EXTRA_VIEW) == VIEW_CLIENT
        setContent {
            SynergyOneTheme {
                if (isClient) ClientDemoScreen() else TeamDemoScreen()
            }
        }
    }

    companion object {
        const val EXTRA_VIEW = "demo_view"
        const val VIEW_CLIENT = "client"
    }
}
