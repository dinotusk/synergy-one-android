package br.com.synergyone.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Puxar para atualizar. Atualiza em segundo plano: o dado atual fica na tela até o novo chegar; se a
 * atualização falhar, o dado antigo permanece e um aviso discreto oferece tentar de novo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(loaders: List<LoaderViewModel<*>>, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val refreshing = loaders.map { it.refreshing.collectAsState().value }.any { it }
    val failed = loaders.map { it.refreshFailed.collectAsState().value }.any { it }
    PullRefreshBox(refreshing, failed, onRefresh = { loaders.forEach { it.refresh() } }, modifier, content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefreshBox(refreshing: Boolean, failed: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh) { content() }
        if (failed && !refreshing) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).navigationBarsPadding().semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(modifier = Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Não foi possível atualizar. Mostrando o que já estava carregado.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp))
                    TextButton(onClick = onRefresh) { Text("Tentar") }
                }
            }
        }
    }
}
