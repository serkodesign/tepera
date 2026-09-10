package com.serkodesign.tepera.ui.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * FR-D.8: "тепловий" графік доби — не показник висоти (bar chart), а КОЛІР-інтенсивність
 * (справжня теплова метафора): 24 однакові за розміром смужки, кожна тоном "Online"-кольору
 * (той самий accent, що сегмент Online на шкалі структури дня) від майже прозорого (година без
 * використання) до насиченого (пікова година). Показує факт — жодного коментаря/поради поверх
 * (FR-D.8: "показати патерн — факт, людина робить висновок сама"). Спільна для компактної
 * картки на Home (PatternMiniCard) і повної картки на Stats — різниться лише [height].
 */
@Composable
fun HourlyHeatStrip(hourlyMinutes: List<Int>, modifier: Modifier = Modifier, height: Dp = 28.dp) {
    val maxMinutes = (hourlyMinutes.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        hourlyMinutes.forEach { minutes ->
            val intensity = (minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(TeperaPalette.onlineCard.copy(alpha = 0.12f + intensity * 0.78f))
            )
        }
    }
}
