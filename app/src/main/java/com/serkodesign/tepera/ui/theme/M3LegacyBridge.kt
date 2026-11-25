package com.serkodesign.tepera.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Дизайн-система v3.1, ітерація I08: «мостик» для екранів, які ще не перенесені на `MaterialTheme.colorScheme` /
 * [M3Extra]. Старі екрани читають кольори через `LocalTeperaColors` / `TeperaPalette`; цей набір будується з
 * [RedesignLightColors]/[RedesignDarkColors], де поля, що мають відповідник у таблиці 4 плану
 * (`CLAUDE-CODE-PLAN-m3-v31.md`), підмінені на значення [M3Palette]. Так неперенесені екрани виглядають
 * узгоджено з новою темою без правок у їхньому коді. Поля без відповідника (навбар, шкала доби, hero тощо)
 * лишаються зі старого набору. Використовується лише в режимах Світла/Темна/Системна ([TeperaTheme]);
 * «Світанок»/«За часом» не чіпаємо (I25l). Видаляється в I27, коли не лишиться використань [TeperaPalette].
 */
fun m3BridgeColors(dark: Boolean): TeperaColors {
    val base = if (dark) RedesignDarkColors else RedesignLightColors
    val m = if (dark) TeperaDarkColors else TeperaLightColors
    val x = if (dark) DarkTeperaExtraColors else LightTeperaExtraColors
    val bridged = base.copy(
        // bg/gradient-*, bg/blob-*
        backgroundTop = x.bgGradientTop,
        backgroundBottom = x.bgGradientBottom,
        backgroundBlobTopRight = x.bgBlobWarm,
        backgroundBlobBottomLeft = x.bgBlobCool,
        // brand, button/primary, text/brand-dark, border/brand → primary.
        // ink у темній темі лишається світлим текстом (primary там м'ятний і не годиться для основного тексту).
        ink = if (dark) m.onSurface else m.primary,
        brand = m.primary,
        borderBrand = m.primary,
        primaryFill = m.primary,
        onPrimary = m.onPrimary,
        // text/primary, chip/on-unselected → onSurface; text/secondary, text/placeholder → onSurfaceVariant
        textPrimary = m.onSurface,
        chipContent = m.onSurface,
        textSecondary = m.onSurfaceVariant,
        textPlaceholder = m.onSurfaceVariant,
        textOnDark = x.onDark,
        // surface/card, surface/default, chip/unselected → card
        card = x.card,
        cardActive = x.card,
        chipFill = x.card,
        // surface/brand-light, switch/track-off → surfaceContainerHighest; surface/dialog → surfaceContainerHigh
        surfaceBrandLight = m.surfaceContainerHighest,
        switchTrackOff = m.surfaceContainerHighest,
        dialogSurface = m.surfaceContainerHigh,
        // surface/glass → glass; header/circle-button → glassStrong
        listItem = x.glass,
        iconButtonFill = x.glassStrong,
        // border/default, border/strong → outline; text/error, border/error → error
        borderDefault = m.outline,
        borderStrong = m.outline,
        textError = m.error,
        borderError = m.error
    )
    // Темна тема: фон за Figma "Tepera" 1233:144 — градієнт #0c1d13 → #070a08 (на 45%) → #050605 і одне зелене світіння
    // (#17663A 50%) зверху зліва замість двох плям.
    return if (dark) {
        bridged.copy(
            // М'якший, розтягнутий за запитом власника: середня зупинка на 65% (у макеті 45%) і трохи зеленіша (#09130d).
            backgroundStops = listOf(x.bgGradientTop, Color(0xFF09130D), x.bgGradientBottom),
            backgroundStopPositions = listOf(0f, 0.65f, 1f),
            backgroundGlow = Color(0x8017663A),
            backgroundBlobTopRight = null,
            backgroundBlobBottomLeft = null
        )
    } else bridged
}
