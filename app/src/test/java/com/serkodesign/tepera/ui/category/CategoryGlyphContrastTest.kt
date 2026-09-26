package com.serkodesign.tepera.ui.category

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Гліф категорії на плашці (колір категорії @20% поверх світлої поверхні) має мати контраст ≥ 3:1
 * (WCAG 2.x 1.4.11). Формула контрасту написана в тесті окремо (за визначенням WCAG), а не береться
 * з коду, що перевіряється.
 */
class CategoryGlyphContrastTest {

    // Кольори дефолтних категорій (DefaultCategories.kt) і палітра кастомних (customCategoryColorChoices).
    private val defaults = listOf(0xFF00B938, 0xFFD28FDF, 0xFF00D8CD, 0xFFFD5B5E, 0xFFFF73D0, 0xFFA362FF)
    private val custom = listOf(0xFF4E7A51, 0xFF4A6FA5, 0xFFB08968, 0xFFC9704F, 0xFF5C6B73, 0xFF7A5C7A, 0xFF8A8F5C)
    private val surface = Color(0xFFEBFAE6)

    private fun channel(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun relativeLuminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun wcagContrast(a: Color, b: Color): Double {
        val l1 = relativeLuminance(a)
        val l2 = relativeLuminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun badgeOf(base: Color, alpha: Float): Color {
        val a = alpha
        return Color(
            red = base.red * a + surface.red * (1 - a),
            green = base.green * a + surface.green * (1 - a),
            blue = base.blue * a + surface.blue * (1 - a)
        )
    }

    @Test
    fun everyCategoryGlyphReachesThreeToOne() {
        for (hex in defaults + custom) {
            val base = Color(hex)
            for (alpha in listOf(0.2f, 0.5f)) {
                val glyph = categoryGlyphColor(base, alpha)
                val ratio = wcagContrast(glyph, badgeOf(base, alpha))
                assertTrue("#${hex.toString(16)} @$alpha: контраст $ratio < 3", ratio >= 3.0)
            }
        }
    }

    @Test
    fun alreadyContrastingColorIsLeftUntouched() {
        val dark = Color(0xFF4E7A51)
        assertEquals(dark, categoryGlyphColor(dark, 0.2f))
    }

    @Test
    fun brightColorIsDarkenedNotReplaced() {
        val bright = Color(0xFF00D8CD)
        val glyph = categoryGlyphColor(bright, 0.2f)
        assertTrue(relativeLuminance(glyph) < relativeLuminance(bright))
        // Тон лишається тим самим: співвідношення каналів зберігається (лише темнішає до чорного).
        assertTrue(glyph.green >= glyph.red && glyph.blue >= glyph.red)
    }
}
