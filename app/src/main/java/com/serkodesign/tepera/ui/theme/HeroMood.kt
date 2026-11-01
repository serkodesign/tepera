package com.serkodesign.tepera.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalTime

/** Відтінок і положення світіння hero для пори доби. Залежить лише від годинника, не від дій людини. */
private class HeroMood(val glow: Color, val cx: Float, val cy: Float)

private fun heroMoodFor(hour: Int): HeroMood = when (hour) {
    in 5..10 -> HeroMood(Color(0xFFE8B96A), 0.15f, 1.1f)   // ранок: тепле янтарне, знизу зліва
    in 11..16 -> HeroMood(Color(0xFF9FE3C8), 0.50f, -0.2f) // день: м'ятне, зверху
    in 17..21 -> HeroMood(Color(0xFFE39A8F), 0.90f, 1.1f)  // вечір: приглушено-рожеве, знизу справа
    else -> HeroMood(Color(0xFF7FA8E0), 0.80f, -0.1f)      // ніч: холодне синє, зверху справа
}

/**
 * Фон hero-картки: звичайний [TeperaPalette.heroSurface] + м'який радіальний відблиск за порою доби (варіант B із
 * макета). Без кольорових порогів і без зв'язку з даними користувача (FR-P). З "Додатковими анімаціями" відблиск
 * повільно "дихає" (радіус ±8%, 7 с), без них стоїть нерухомо.
 */
@Composable
fun Modifier.heroMoodBackground(showBody: Boolean = true): Modifier {
    val surface = TeperaPalette.heroSurface
    val realHour = remember { LocalTime.now().hour }
    val hour = DebugClock.override.value?.div(60) ?: realHour
    val mood = remember(hour) { heroMoodFor(hour) }
    val colors = LocalTeperaColors.current
    val alpha = if (colors.isDark) 0.40f else 0.28f
    val breath = if (extraAnimationsEnabled()) {
        rememberInfiniteTransition(label = "heroGlow").animateFloat(
            initialValue = 0.92f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "heroGlowScale"
        )
    } else null
    if (colors.isDawn) {
        // «Світанок»: hero без картки, у куті сонце (5–17 год) або місяць; «дихає» разом із ореолом.
        val isMoon = hour !in 5..17
        return drawBehind {
            // Картка без диска (NextCard) отримує скляну заливку, щоб її було чітко видно на небі.
            if (!showBody) drawRect(surface)
            val scale = breath?.value ?: 1f
            val r = 46.dp.toPx()
            val c = Offset(size.width - r - 4.dp.toPx(), r - 2.dp.toPx())
            val halo = if (isMoon) Color(0xFFB9B4E8) else Color(0xFFF2A6C8)
            drawCircle(
                Brush.radialGradient(listOf(halo.copy(alpha = 0.40f), halo.copy(alpha = 0f)), center = c, radius = r * 1.9f * scale),
                radius = r * 1.9f * scale, center = c
            )
            // Картки з власними іконками праворуч (NextCard) малюють лише ореол, без диска.
            if (!showBody) return@drawBehind
            val body = if (isMoon) {
                listOf(Color(0xFFF4F1FF), Color(0xFFB9B4E8), Color(0xFF8E88C9))
            } else {
                listOf(Color(0xFFFFF3C4), Color(0xFFFFD98A), Color(0xFFF2A6C8))
            }
            drawCircle(Brush.radialGradient(body, center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.4f), radius = r, center = c)
        }
    }
    return drawBehind {
        drawRect(surface)
        val scale = breath?.value ?: 1f
        drawRect(
            Brush.radialGradient(
                colors = listOf(mood.glow.copy(alpha = alpha), mood.glow.copy(alpha = 0f)),
                center = Offset(size.width * mood.cx, size.height * mood.cy),
                radius = size.width * 0.75f * scale
            )
        )
    }
}
