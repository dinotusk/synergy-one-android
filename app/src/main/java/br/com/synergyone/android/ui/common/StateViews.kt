package br.com.synergyone.android.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.components.SynergyCard

/** Esqueleto pulsante: dá ideia da forma do conteúdo enquanto carrega (melhor que um spinner). */
@Composable
fun SkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(pulse)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    // Sem recuo horizontal próprio: a tela que hospeda já o aplica (antes o esqueleto ficava mais estreito que o conteúdo).
    Column(modifier = modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBlock(120.dp)
        SkeletonBlock(88.dp)
        SkeletonBlock(160.dp)
    }
}

@Composable
fun ErrorView(
    message: String,
    sessionExpired: Boolean,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (sessionExpired) "Sessão encerrada" else "Não foi possível carregar",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (sessionExpired) {
            Button(onClick = onSignIn) { Text("Entrar novamente") }
        } else {
            OutlinedButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

/** Resolve os três estados de um [LoadState] e entrega o conteúdo pronto. */
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        LoadState.Loading -> LoadingView()
        is LoadState.Error -> ErrorView(state.message, state.sessionExpired, onRetry, onSignIn)
        is LoadState.Loaded -> content(state.data)
    }
}

@Composable
fun InfoNote(title: String, detail: String, modifier: Modifier = Modifier) {
    SynergyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
