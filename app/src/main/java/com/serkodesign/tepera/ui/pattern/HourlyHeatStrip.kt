package com.serkodesign.tepera.ui.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * FR-D.8: "тепловий" графік доби, редизайн за Figma "App concept"
 * (k6s4prQ9oK9x2uUvzHRghR, node 154:287, "Day usage") — замінює попередню неперервну
 * альфа-шкалу (24 бари, інтенсивність відносно піку години) на дискретну сітку 12×2 (24
 * години) з фіксованими кошиками АБСОЛЮТНИХ хвилин Online-часу за годину (0-15/15-30/30-45/
 * 45-60) і легендою під нею — той самий принцип "показати факт, без коментаря" (FR-D.8), лише
 * тепер зчитується явним кольором-кошиком, а не відносною інтенсивністю. Спільна для
 * компактної картки на Home (`PatternMiniCard`) і повної картки на Stats (`StatsScreen`) —
 * за прямим рішенням користувача ОБИДВІ версії ідентичні макету (без окремого "стиснутого"
 * варіанта чи додаткових годинних підписів, які показувала попередня версія на Stats).
 *
 * [hourlyMinutes] — 24 значення (Online-хвилини за годину), або `null`, коли даних замало
 * (`PatternUiState.hasEnoughData == false`) — тоді всі 24 клітинки рендеряться в нейтральному
 * "без даних" стилі з легенди, замість того, щоб ховати сітку цілком. Нуль хвилин у РЕАЛЬНИХ
 * даних — це кошик "0-15" (людина була офлайн ту годину — валідний факт), не "без даних";
 * "без даних" стосується лише випадку, коли даних немає взагалі.
 */
@Composable
fun HourlyHeatGrid(hourlyMinutes: List<Int>?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeatGridRows(hourlyMinutes)
        HeatGridLegend()
    }
}

@Composable
private fun HeatGridRows(hourlyMinutes: List<Int>?) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(0 until 12, 12 until 24).forEach { hourRange ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                hourRange.forEach { hour ->
                    HeatCell(minutes = hourlyMinutes?.getOrNull(hour), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeatCell(minutes: Int?, modifier: Modifier = Modifier) {
    val cellShape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cellShape)
            .then(
                if (minutes == null) {
                    Modifier
                        .background(TeperaPalette.heatmapNoDataFill)
                        .border(1.dp, TeperaPalette.heatmapNoDataBorder, cellShape)
                } else {
                    Modifier.background(heatBucketColor(minutes))
                }
            )
    )
}

/**
 * Абсолютні кошики хвилин/годину — 0-1/1-15/15-30/30-45/45-60. Колишній кошик "0-15" за
 * запитом користувача розділено: рівно 0 хв — нейтральний сірий, 1-14 хв — амбер 15%.
 */
private fun heatBucketColor(minutes: Int): Color = when {
    minutes < 1 -> TeperaPalette.heatmapLowBucket
    minutes < 15 -> TeperaPalette.heatmapAmber.copy(alpha = 0.15f)
    minutes < 30 -> TeperaPalette.heatmapAmber.copy(alpha = 0.45f)
    minutes < 45 -> TeperaPalette.heatmapAmber.copy(alpha = 0.7f)
    else -> TeperaPalette.heatmapAmber
}

@Composable
private fun HeatGridLegend() {
    // П'ять елементів у рядок (пункт "Без даних" прибрано за запитом користувача — стан "немає даних"
    // і так пояснює текст картки): без рівних ваг і без переносу підписів (softWrap = false), інакше
    // "15-30"/"30-45"/"45-60" ламались на два рядки; вільне місце розподіляє SpaceBetween.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(
            TeperaPalette.heatmapLowBucket to "0-1",
            TeperaPalette.heatmapAmber.copy(alpha = 0.15f) to "1-15",
            TeperaPalette.heatmapAmber.copy(alpha = 0.45f) to "15-30",
            TeperaPalette.heatmapAmber.copy(alpha = 0.7f) to "30-45",
            TeperaPalette.heatmapAmber to "45-60"
        ).forEach { (color, label) ->
            LegendItem {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Text(label, style = MaterialTheme.typography.labelSmall, softWrap = false)
            }
        }
    }
}

@Composable
private fun LegendItem(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
