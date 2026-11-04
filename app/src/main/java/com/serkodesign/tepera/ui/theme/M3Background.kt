package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview

/**
 * Фон застосунку за дизайн-системою v3.1 (розділ 3.2 плану): вертикальний градієнт `bgGradientTop` →
 * `bgGradientBottom` і дві м'які плями діаметром ~604dp на макеті 375×812 — `bgBlobWarm` із центром у ~(369, 20)
 * (6dp від правого й 20dp від верхнього краю) та `bgBlobCool` із центром у ~(9, 781) (9dp від лівого й 31dp від
 * нижнього краю). Геометрія і розмиття — ті самі, що в наявному `teperaGradientBackground()` (радіус 302dp,
 * σ = 97.55dp, [drawBlurredBlob]); відрізняється лише джерело кольорів — [M3Extra].
 *
 * Паралельний варіант: `teperaGradientBackground()` не змінювався, бо його ж використовують режими «Світанок»/«За часом»
 * (небо з `LocalTeperaColors`). Підключення в `TeperaNavHost` (з відкатом на старий модифікатор для цих режимів) — I25d.
 */
@Composable
fun Modifier.m3Background(): Modifier {
    val colors = M3Extra.colors
    val gradient = Brush.verticalGradient(listOf(colors.bgGradientTop, colors.bgGradientBottom))
    val warm = colors.bgBlobWarm
    val cool = colors.bgBlobCool
    return this
        .fillMaxSize()
        .background(gradient)
        .drawBehind {
            val d = density
            drawBlurredBlob(center = Offset(size.width - 6f * d, 20f * d), radius = 302f * d, sigma = 97.55f * d, color = warm)
            drawBlurredBlob(center = Offset(9f * d, size.height - 31f * d), radius = 302f * d, sigma = 97.55f * d, color = cool)
        }
}

/** Контейнер на весь екран із фоном [m3Background]. */
@Composable
fun M3Background(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(modifier.m3Background()) { content() }
}

@Preview(name = "M3Background · Light", widthDp = 375, heightDp = 812)
@Composable
private fun M3BackgroundLightPreview() {
    androidx.compose.runtime.CompositionLocalProvider(LocalTeperaExtraColors provides LightTeperaExtraColors) {
        M3Background(Modifier.fillMaxSize())
    }
}

@Preview(name = "M3Background · Dark", widthDp = 375, heightDp = 812)
@Composable
private fun M3BackgroundDarkPreview() {
    androidx.compose.runtime.CompositionLocalProvider(LocalTeperaExtraColors provides DarkTeperaExtraColors) {
        M3Background(Modifier.fillMaxSize())
    }
}
