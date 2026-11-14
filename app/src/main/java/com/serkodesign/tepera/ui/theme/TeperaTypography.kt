package com.serkodesign.tepera.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R

/** Golos Text (variable `res/font/golos_text.ttf`): Regular 400, Medium 500. */
val GolosTextFamily: FontFamily = FontFamily(
    Font(R.font.golos_text, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.golos_text, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
)

private fun m3Style(size: Int, line: Int, spacing: Float, weight: FontWeight): TextStyle = TextStyle(
    fontFamily = GolosTextFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

/**
 * Дизайн-система v3.1: шкала M3 на Golos Text (розділ 2.3 плану). Display/Headline/Title/Label — Medium,
 * Body — Normal. Поки не підключена (підключення — ітерація I07). `Onboarding/Numeral` (156sp) лишається
 * окремим кастомним стилем і сюди не входить.
 */
val TeperaTypography = Typography(
    displayLarge = m3Style(57, 64, -0.86f, FontWeight.Medium),
    displayMedium = m3Style(45, 52, -0.68f, FontWeight.Medium),
    displaySmall = m3Style(36, 44, -0.36f, FontWeight.Medium),
    headlineLarge = m3Style(32, 40, -0.32f, FontWeight.Medium),
    headlineMedium = m3Style(28, 36, -0.28f, FontWeight.Medium),
    headlineSmall = m3Style(24, 32, -0.12f, FontWeight.Medium),
    titleLarge = m3Style(22, 28, -0.11f, FontWeight.Medium),
    titleMedium = m3Style(16, 24, 0f, FontWeight.Medium),
    titleSmall = m3Style(14, 20, 0f, FontWeight.Medium),
    // Body: міжрядковий інтервал ~1.25 (було 1.43–1.5): дворядкові підписи в списках виглядали розрідженими.
    bodyLarge = m3Style(16, 20, 0f, FontWeight.Normal),
    bodyMedium = m3Style(14, 18, 0f, FontWeight.Normal),
    bodySmall = m3Style(12, 15, 0f, FontWeight.Normal),
    labelLarge = m3Style(14, 20, 0f, FontWeight.Medium),
    labelMedium = m3Style(12, 16, 0f, FontWeight.Medium),
    labelSmall = m3Style(11, 16, 0.11f, FontWeight.Medium)
)
