package br.com.synergyone.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import br.com.synergyone.android.ui.components.DashboardBottomBar
import br.com.synergyone.android.ui.components.DashboardTab

/**
 * Estrutura comum das áreas de equipe e cliente: barra inferior + conteúdo da aba selecionada.
 * O brilho violeta no topo é o "page-gradient" do site, bem discreto.
 */
@Composable
fun HomeScaffold(tabs: List<DashboardTab>, content: @Composable (selected: Int, select: (Int) -> Unit) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    // Voltar numa aba secundária leva ao Termômetro (aba principal) antes de sair do app.
    BackHandler(enabled = selected != 0) { selected = 0 }
    val glow = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { DashboardBottomBar(tabs, selected) { selected = it } },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(glow.copy(alpha = 0.10f), Color.Transparent), Offset(0f, 0f), 900f))
                .padding(padding),
        ) {
            // Cada aba guarda o próprio estado (rolagem, empresa, período, filtros) ao trocar de aba:
            // voltar a ela não recomeça do zero nem refaz a consulta. Fade curto; sem movimento se o
            // sistema desliga animações (a escala de duração zerada vale para o tween).
            val holder = rememberSaveableStateHolder()
            Crossfade(targetState = selected, animationSpec = tween(120), label = "aba") { tab ->
                holder.SaveableStateProvider(tab) { content(tab) { selected = it } }
            }
        }
    }
}
