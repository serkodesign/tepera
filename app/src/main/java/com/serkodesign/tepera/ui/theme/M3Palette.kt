package com.serkodesign.tepera.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Дизайн-система v3.1 (Material 3): сирі hex-значення з Figma-сторінки "v3.1 · M3".
 * Лише константи — без логіки. Підключаються в [M3ColorSchemes] (ролі M3) та [TeperaExtraColors]
 * (ролі Tepera поза M3). Єдине місце, де в UI-коді дозволено `Color(0x…)`.
 *
 * Не плутати з наявними `TeperaPalette` / `TeperaColors` (старий редизайн, режими Dawn/Time) —
 * вони живуть паралельно, доки екрани не перенесені (план `CLAUDE-CODE-PLAN-m3-v31.md`).
 * Формат прозорості: `#RRGGBB@NN%` у Figma → альфа-байт ARGB (8% = 0x14, 10% = 0x1A, 14% = 0x24,
 * 30% = 0x4D, 62% = 0x9E, 80% = 0xCC).
 */
object M3Palette {

    /** Ролі M3 → `MaterialTheme.colorScheme` (розділ 2.1 плану). */
    object Light {
        val primary = Color(0xFF003926)
        val onPrimary = Color(0xFFFFFFFF)
        val primaryContainer = Color(0xFFDCF6ED)
        val onPrimaryContainer = Color(0xFF003926)
        val surface = Color(0xFFFAF9F9)
        val surfaceDim = Color(0xFFDADADA)
        val surfaceBright = Color(0xFFFAF9F9)
        val surfaceContainerLowest = Color(0xFFFFFFFF)
        val surfaceContainerLow = Color(0xFFF4F3F4)
        val surfaceContainer = Color(0xFFEEEEEE)
        val surfaceContainerHigh = Color(0xFFE8E8E8)
        val surfaceContainerHighest = Color(0xFFE3E2E2)
        val onSurface = Color(0xFF1A1C1C)
        val onSurfaceVariant = Color(0xFF434749)
        val outline = Color(0xFF737879)
        val outlineVariant = Color(0xFFC3C7C8)
        val inverseSurface = Color(0xFF2F3131)
        val inverseOnSurface = Color(0xFFF1F0F1)
        val inversePrimary = Color(0xFF83D8AA)
        val error = Color(0xFFBA1A1A)
        val onError = Color(0xFFFFFFFF)
        val errorContainer = Color(0xFFFFDAD6)
        val onErrorContainer = Color(0xFF410002)
        val scrim = Color(0xFF000000)
    }

    object Dark {
        val primary = Color(0xFF83D8AA)
        val onPrimary = Color(0xFF003926)
        val primaryContainer = Color(0xFF005234)
        val onPrimaryContainer = Color(0xFF9EF4C5)
        val surface = Color(0xFF121414)
        val surfaceDim = Color(0xFF121414)
        val surfaceBright = Color(0xFF38393A)
        val surfaceContainerLowest = Color(0xFF0D0E0F)
        val surfaceContainerLow = Color(0xFF1A1C1C)
        val surfaceContainer = Color(0xFF1E2020)
        val surfaceContainerHigh = Color(0xFF292A2B)
        val surfaceContainerHighest = Color(0xFF333535)
        val onSurface = Color(0xFFE3E2E2)
        val onSurfaceVariant = Color(0xFFC3C7C8)
        val outline = Color(0xFF8D9193)
        val outlineVariant = Color(0xFF434749)
        val inverseSurface = Color(0xFFE3E2E2)
        val inverseOnSurface = Color(0xFF2F3131)
        val inversePrimary = Color(0xFF003926)
        val error = Color(0xFFFFB4AB)
        val onError = Color(0xFF690005)
        val errorContainer = Color(0xFF93000A)
        val onErrorContainer = Color(0xFFFFDAD6)
        val scrim = Color(0xFF000000)
    }

    /** Ролі Tepera поза M3 → [TeperaExtraColors] (розділ 2.2 плану). */
    object ExtraLight {
        val signal = Color(0xFF805600)
        val onSignal = Color(0xFFFFFFFF)
        val signalContainer = Color(0xFFFFDDB0)
        val onSignalContainer = Color(0xFF614000)
        val signalVivid = Color(0xFFE69D00)
        val inverseSignal = Color(0xFFFFBA47)
        val card = Color(0xFFFFFFFF)
        val cardStroke = Color(0xFFFFFFFF)
        val onDark = Color(0xFFFFFFFF)
        val glass = Color(0x9EFFFFFF) // 62%
        val glassStrong = Color(0xCCFFFFFF) // 80%
        val bgGradientTop = Color(0xFFF9F0DF)
        val bgGradientBottom = Color(0xFFEDE9E8)
        val bgBlobWarm = Color(0xFFFFF2D2)
        val bgBlobCool = Color(0x4DC2BDF6) // 30%
        val dataHeat0 = Color(0xFFE8E8E8)
        val dataHeat1 = Color(0xFFFFEEDB)
        val dataHeat2 = Color(0xFFFFDDB0)
        val dataHeat3 = Color(0xFFFFCC81)
        val dataHeat4 = Color(0xFFF5AA1A)
        val dataHeat5 = Color(0xFFE69D00)
        val dataTrack = Color(0xFFE3E2E2)
        val dataNature = Color(0xFF5D905A)
        val dataReading = Color(0xFFA373AC)
        val dataHobby = Color(0xFF30928B)
        val dataMovement = Color(0xFFBF6D6A)
        val dataSocial = Color(0xFFB26E97)
        val dataErrands = Color(0xFF9278B8)
        val dataOnline = Color(0xFF9C812B)
    }

    object ExtraDark {
        val signal = Color(0xFFFFBA47)
        val onSignal = Color(0xFF442B00)
        val signalContainer = Color(0xFF614000)
        val onSignalContainer = Color(0xFFFFDDB0)
        val signalVivid = Color(0xFFFFBA47)
        val inverseSignal = Color(0xFF805600)
        val card = Color(0xFF292A2B)
        val cardStroke = Color(0x14FFFFFF) // 8%
        val onDark = Color(0xFFFFFFFF)
        val glass = Color(0x14FFFFFF) // 8%
        val glassStrong = Color(0x24FFFFFF) // 14%
        val bgGradientTop = Color(0xFF1B1B1D)
        val bgGradientBottom = Color(0xFF03190C)
        val bgBlobWarm = Color(0x1AFFF2D2) // 10%
        val bgBlobCool = Color(0x1AC2BDF6) // 10%
        val dataHeat0 = Color(0xFF2F3131)
        val dataHeat1 = Color(0xFF442B00)
        val dataHeat2 = Color(0xFF614000)
        val dataHeat3 = Color(0xFF805600)
        val dataHeat4 = Color(0xFFC38400)
        val dataHeat5 = Color(0xFFFFBA47)
        val dataTrack = Color(0xFF38393A)
        val dataNature = Color(0xFF96C590)
        val dataReading = Color(0xFFD8A9DE)
        val dataHobby = Color(0xFF79C6BF)
        val dataMovement = Color(0xFFF6A3A0)
        val dataSocial = Color(0xFFE8A5CB)
        val dataErrands = Color(0xFFC7AEE9)
        val dataOnline = Color(0xFFD3B666)
    }
}
