package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.ui.theme.TeperaPalette
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Спільні елементи екранів v3.0 (Figma "App concept", сторінка v3.0: `v3/HeatWeek` 909:144, `v3/RiskWindowCard`
 * 909:380). Усі кольори — токени [TeperaPalette] (світла й темна тема), не значення з макета напряму.
 */

/** Скляна картка v3: радіус 24, поле 16/14 (макет), обвідка — якщо тема її має. */
@Composable
internal fun V3Card(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    radius: Dp = 24.dp,
    spacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = TeperaPalette.activityCardIdleBorder
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TeperaPalette.cardSurface)
            .let { if (border != null) it.border(1.dp, border, shape) else it }
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}


internal fun formatWindowTime(startMinute: Int, endMinute: Int): String =
    String.format(Locale.ROOT, "%02d:%02d – %02d:%02d", startMinute / 60 % 24, startMinute % 60, endMinute / 60 % 24, endMinute % 60)

/**
 * `v3/HeatWeek`: рядки — дні (підпис дня тижня), колонки — 24 години, інтенсивність — кошики з токенів теплової карти,
 * години ризику — пунктирні рамки (для вікна через північ — дві, як у макеті).
 */
@Composable
fun HeatWeek(
    dates: List<LocalDate>,
    grid: Array<IntArray>,
    windows: List<Pair<Int, Int>>,
    description: String,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val ramp = TeperaPalette.heatmapRamp
    val empty = TeperaPalette.heatmapNoDataFill
    val dashColor = TeperaPalette.brandAccent
    val secondary = TeperaPalette.textSecondary
    val rows = dates.size

    V3Card(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        padding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
        spacing = 6.dp
    ) {
        Row {
            Column(Modifier.width(22.dp)) {
                dates.forEach { date ->
                    Box(Modifier.height(14.dp)) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase() },
                            color = secondary, fontSize = 11.sp, lineHeight = 12.sp, maxLines = 1
                        )
                    }
                }
            }
            Canvas(Modifier.weight(1f).height((rows * 14 - 1).dp)) {
                val pitch = size.width / HeatGrid.HOURS
                val cellW = pitch - 1.dp.toPx()
                val cellH = 13.dp.toPx()
                val rowPitch = 14.dp.toPx()
                val radius = CornerRadius(2.dp.toPx())
                for (r in 0 until rows) for (c in 0 until HeatGrid.HOURS) {
                    val bucket = HeatGrid.bucket(grid.getOrNull(r)?.getOrNull(c) ?: 0)
                    drawRoundRect(
                        color = if (bucket == 0) empty else ramp[bucket - 1],
                        topLeft = Offset(c * pitch, r * rowPitch),
                        size = Size(cellW, cellH),
                        cornerRadius = radius
                    )
                }
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                )
                for ((start, end) in windows) for (range in HeatGrid.windowColumns(start, end)) {
                    drawRoundRect(
                        color = dashColor,
                        topLeft = Offset(range.first * pitch - 1.dp.toPx(), -2.dp.toPx()),
                        size = Size((range.last - range.first + 1) * pitch + 1.dp.toPx(), rows * rowPitch + 2.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = stroke
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 22.dp).height(14.dp)) {
            listOf(0, 6, 12, 18, 23).forEach { hour ->
                Text(
                    text = String.format(Locale.ROOT, "%02d", hour),
                    color = secondary, fontSize = 11.sp, lineHeight = 12.sp,
                    modifier = Modifier.offset(x = maxWidth * (hour / HeatGrid.HOURS.toFloat()))
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendItem(stringResource(R.string.v3_heat_legend_feeds)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(TeperaPalette.heatmapAmber))
            }
            LegendItem(stringResource(R.string.v3_heat_legend_window)) {
                Canvas(Modifier.size(10.dp)) {
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, marker: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        marker()
        Text(label, color = TeperaPalette.textSecondary, fontSize = 11.sp, lineHeight = 12.sp)
    }
}

/** `v3/RiskWindowCard`, стан «Без плану» (кнопка «Скласти план» з'явиться разом з конструктором плану, L5). */
@Composable
fun RiskWindowCard(window: RiskWindowUi, modifier: Modifier = Modifier) {
    val primary = TeperaPalette.textPrimary
    val secondary = TeperaPalette.textSecondary
    val recurrence = stringResource(
        if (window.dayType == DayType.WEEKDAYS) R.string.v3_card_recurrence_weekdays else R.string.v3_card_recurrence_days,
        window.recurrenceDays, window.observedDays
    )
    val part = stringResource(partOfDayRes(window.partOfDay))
    val meta = if (window.appLabels.isEmpty()) {
        stringResource(R.string.v3_card_meta_no_apps, part, window.avgMinutes)
    } else {
        stringResource(R.string.v3_card_meta_format, part, window.appLabels.joinToString(", "), window.avgMinutes)
    }
    V3Card(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = formatWindowTime(window.startMinute, window.endMinute),
                color = primary, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.clip(RoundedCornerShape(100.dp)).background(TeperaPalette.heatmapAmber.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(recurrence, color = primary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Text(meta, color = secondary, fontSize = 12.sp, lineHeight = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            window.dots.forEach { dot ->
                Box(
                    Modifier.size(8.dp).clip(CircleShape).background(
                        when (dot) {
                            DotState.HIT -> TeperaPalette.heatmapAmber
                            DotState.MISS -> TeperaPalette.heatmapNoDataFill
                            DotState.NOT_APPLICABLE -> TeperaPalette.heatmapNoDataFill.copy(alpha = 0.4f)
                        }
                    )
                )
            }
        }
        HorizontalDivider(color = TeperaPalette.borderDefault)
        Text(stringResource(R.string.v3_card_no_plan), color = secondary, fontSize = 14.sp, lineHeight = 18.sp)
    }
}

private fun partOfDayRes(part: PartOfDay): Int = when (part) {
    PartOfDay.MORNING -> R.string.v3_part_morning
    PartOfDay.LUNCH -> R.string.v3_part_lunch
    PartOfDay.DAY -> R.string.v3_part_day
    PartOfDay.EVENING -> R.string.v3_part_evening
    PartOfDay.NIGHT -> R.string.v3_part_night
}
