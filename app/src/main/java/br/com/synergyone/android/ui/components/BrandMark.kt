package br.com.synergyone.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.R

/** Fundo do chip da marca: o mesmo lilás claro do ícone do app (não é branco puro). */
private val MarkChip = Color(0xFFEEEAFC)

/**
 * Marca oficial (o "S" em degradê roxo→azul), sem alterações. Sobre azul-noite ela tem contraste
 * baixo (~1,2:1 no topo escuro), então vai num chip lilás claro e pequeno, igual ao ícone do app.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.30f),
        color = MarkChip,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_synergy),
            contentDescription = stringResource(R.string.login_brand_mark_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(size * 0.2f),
        )
    }
}
