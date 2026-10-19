package br.com.synergyone.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** Cores do site que o Material3 não tem como papel próprio (status, borda, faixa de marca). */
@Immutable
data class SynergyExtras(
    val success: Color,
    val warning: Color,
    val info: Color,
    val danger: Color,
    val lilac: Color,
    val surface2: Color,
    val border: Color,
    val primaryGlow: Color,
    /** Violeta para **texto pequeno** (4,5:1+): no escuro é mais claro que o primary. */
    val primaryText: Color,
)

private val LightExtras = SynergyExtras(
    success = SuccessLight, warning = WarningLight, info = InfoLight, danger = DestructiveLight, lilac = LilacLight,
    surface2 = Surface2Light, border = BorderLight, primaryGlow = PrimaryGlowLight, primaryText = PrimaryLight,
)
private val DarkExtras = SynergyExtras(
    success = SuccessDark, warning = WarningDark, info = InfoDark, danger = DestructiveDark, lilac = LilacDark,
    surface2 = Surface2Dark, border = BorderDark, primaryGlow = PrimaryGlowDark, primaryText = PrimaryGlowDark,
)

private val LocalExtras = staticCompositionLocalOf { LightExtras }

val MaterialTheme.synergy: SynergyExtras
    @Composable @ReadOnlyComposable get() = LocalExtras.current

private val LightColors = lightColorScheme(
    primary = PrimaryLight, onPrimary = OnPrimaryLight,
    primaryContainer = AccentLight, onPrimaryContainer = OnAccentLight,
    secondary = OnAccentLight, onSecondary = OnPrimaryLight,
    secondaryContainer = SecondaryLight, onSecondaryContainer = OnSecondaryLight,
    background = BackgroundLight, onBackground = ForegroundLight,
    surface = SurfaceLight, onSurface = ForegroundLight,
    surfaceVariant = MutedLight, onSurfaceVariant = OnMutedLight,
    surfaceContainer = Surface2Light, surfaceContainerLow = SurfaceLight, surfaceContainerHigh = Surface2Light,
    outline = OnMutedLight, outlineVariant = BorderLight,
    error = DestructiveLight, onError = OnPrimaryLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark, onPrimary = OnPrimaryDark,
    primaryContainer = AccentDark, onPrimaryContainer = OnAccentDark,
    secondary = PrimaryGlowDark, onSecondary = BackgroundDark,
    secondaryContainer = SecondaryDark, onSecondaryContainer = OnSecondaryDark,
    background = BackgroundDark, onBackground = ForegroundDark,
    surface = SurfaceDark, onSurface = ForegroundDark,
    surfaceVariant = MutedDark, onSurfaceVariant = OnMutedDark,
    surfaceContainer = Surface2Dark, surfaceContainerLow = SurfaceDark, surfaceContainerHigh = Surface2Dark,
    outline = OnMutedDark, outlineVariant = BorderDark,
    error = DestructiveDark, onError = OnPrimaryDark,
)

// Raio base do site: 0.875rem (14dp).
private val SynergyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Tema do Synergy One, alinhado ao design system do site. O padrão é o escuro (azul-noite); o claro
 * é opção do usuário. A cor dinâmica do Android fica desligada para não descaracterizar a marca.
 */
@Composable
fun SynergyOneTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalExtras provides if (darkTheme) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SynergyTypography,
            shapes = SynergyShapes,
            content = content,
        )
    }
}
