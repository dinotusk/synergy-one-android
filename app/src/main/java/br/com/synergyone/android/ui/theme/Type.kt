package br.com.synergyone.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.synergyone.android.R

// Mesmas fontes do site: Sora nos títulos, Manrope no texto (peso 500 como base, igual ao site).
@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val SoraFamily = FontFamily(
    variable(R.font.sora_variable, 600),
    variable(R.font.sora_variable, 700),
)

val ManropeFamily = FontFamily(
    variable(R.font.manrope_variable, 500),
    variable(R.font.manrope_variable, 600),
    variable(R.font.manrope_variable, 700),
)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = SoraFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = (size * -0.02f).sp,
)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Medium) = TextStyle(
    fontFamily = ManropeFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
)

val SynergyTypography = Typography(
    displaySmall = display(32, 40),
    headlineLarge = display(28, 36),
    headlineMedium = display(24, 32),
    headlineSmall = display(20, 28),
    titleLarge = display(18, 26),
    titleMedium = display(16, 22, FontWeight.SemiBold),
    titleSmall = text(14, 20, FontWeight.Bold),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 21),
    bodySmall = text(12, 18),
    labelLarge = text(14, 20, FontWeight.SemiBold),
    labelMedium = text(12, 16, FontWeight.SemiBold),
    labelSmall = text(11, 14, FontWeight.SemiBold),
)
