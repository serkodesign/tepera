package com.serkodesign.tepera.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Дизайн-система v3.1: M3 `ColorScheme` зі значень [M3Palette] (розділ 2.1 плану). Поки ніде не
 * підключена (підключення — ітерація I07). `secondary*` і `tertiary*` дублюють `primary*`, щоб
 * стандартні компоненти M3 не підхоплювали фіолетові кольори за замовчуванням. Dynamic color вимкнено.
 */
val TeperaLightColors: ColorScheme = M3Palette.Light.let { c ->
    lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        inversePrimary = c.inversePrimary,
        secondary = c.primary,
        onSecondary = c.onPrimary,
        secondaryContainer = c.primaryContainer,
        onSecondaryContainer = c.onPrimaryContainer,
        tertiary = c.primary,
        onTertiary = c.onPrimary,
        tertiaryContainer = c.primaryContainer,
        onTertiaryContainer = c.onPrimaryContainer,
        background = c.surface,
        onBackground = c.onSurface,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceContainerHighest,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.primary,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        outline = c.outline,
        outlineVariant = c.outlineVariant,
        scrim = c.scrim,
        surfaceBright = c.surfaceBright,
        surfaceDim = c.surfaceDim,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
        surfaceContainerLow = c.surfaceContainerLow,
        surfaceContainerLowest = c.surfaceContainerLowest,
    )
}

val TeperaDarkColors: ColorScheme = M3Palette.Dark.let { c ->
    darkColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        inversePrimary = c.inversePrimary,
        secondary = c.primary,
        onSecondary = c.onPrimary,
        secondaryContainer = c.primaryContainer,
        onSecondaryContainer = c.onPrimaryContainer,
        tertiary = c.primary,
        onTertiary = c.onPrimary,
        tertiaryContainer = c.primaryContainer,
        onTertiaryContainer = c.onPrimaryContainer,
        background = c.surface,
        onBackground = c.onSurface,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceContainerHighest,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.primary,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        outline = c.outline,
        outlineVariant = c.outlineVariant,
        scrim = c.scrim,
        surfaceBright = c.surfaceBright,
        surfaceDim = c.surfaceDim,
        surfaceContainer = c.surfaceContainer,
        surfaceContainerHigh = c.surfaceContainerHigh,
        surfaceContainerHighest = c.surfaceContainerHighest,
        surfaceContainerLow = c.surfaceContainerLow,
        surfaceContainerLowest = c.surfaceContainerLowest,
    )
}
