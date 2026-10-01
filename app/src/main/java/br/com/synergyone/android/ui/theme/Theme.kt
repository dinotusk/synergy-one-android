package br.com.synergyone.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = SynergyPrimaryLight,
    onPrimary = SynergyOnPrimaryLight,
    primaryContainer = SynergyPrimaryContainerLight,
    onPrimaryContainer = SynergyOnPrimaryContainerLight,
    secondary = SynergySecondaryLight,
    background = SynergyBackgroundLight,
    onBackground = SynergyOnBackgroundLight,
    surface = SynergySurfaceLight,
    onSurface = SynergyOnSurfaceLight,
    surfaceVariant = SynergySurfaceVariantLight,
    outline = SynergyOutlineLight,
    error = SynergyErrorLight,
)

private val DarkColors = darkColorScheme(
    primary = SynergyPrimaryDark,
    onPrimary = SynergyOnPrimaryDark,
    primaryContainer = SynergyPrimaryContainerDark,
    onPrimaryContainer = SynergyOnPrimaryContainerDark,
    secondary = SynergySecondaryDark,
    background = SynergyBackgroundDark,
    onBackground = SynergyOnBackgroundDark,
    surface = SynergySurfaceDark,
    onSurface = SynergyOnSurfaceDark,
    surfaceVariant = SynergySurfaceVariantDark,
    outline = SynergyOutlineDark,
    error = SynergyErrorDark,
)

/**
 * Tema base do Synergy One. A paleta da marca é o padrão para manter uma experiência
 * consistente entre dispositivos; a cor dinâmica pode ser habilitada pontualmente em previews.
 */
@Composable
fun SynergyOneTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SynergyTypography,
        content = content,
    )
}
