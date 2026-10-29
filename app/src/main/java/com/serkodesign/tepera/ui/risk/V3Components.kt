package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaChip
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Спільні елементи екранів v3.0 (Figma "App concept", сторінка v3.0: `v3/HeatWeek` 909:144, `v3/RiskWindowCard`
 * 909:380). Усі кольори — токени [TeperaPalette] (світла й темна тема), не значення з макета напряму.
 */

/** Скляна картка v3: ті самі радіус 16, поле 16 і проміжок 12, що в [com.serkodesign.tepera.ui.theme.TeperaCard]; обвідка — якщо тема її має. */
@Composable
internal fun V3Card(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    radius: Dp = 16.dp,
    spacing: Dp = 12.dp,
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
        padding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        spacing = 8.dp
    ) {
        Row {
            Column(Modifier.width(28.dp)) {
                dates.forEach { date ->
                    Box(Modifier.height(16.dp)) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase() },
                            color = secondary, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1
                        )
                    }
                }
            }
            Canvas(Modifier.weight(1f).height((rows * 16 - 1).dp)) {
                val pitch = size.width / HeatGrid.HOURS
                val cellW = pitch - 1.dp.toPx()
                val cellH = 15.dp.toPx()
                val rowPitch = 16.dp.toPx()
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
        // Підпис години — по центру її стовпця, не лівим краєм; крайні не виходять за картку.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 28.dp).height(16.dp)) {
            val labelWidth = 20.dp
            listOf(0, 6, 12, 18, 23).forEach { hour ->
                val center = maxWidth * ((hour + 0.5f) / HeatGrid.HOURS)
                Text(
                    text = String.format(Locale.ROOT, "%02d", hour),
                    color = secondary, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(labelWidth).offset(x = (center - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth))
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
        LegendScale(ramp)
    }
}

@Composable
private fun LegendItem(label: String, marker: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        marker()
        Text(label, color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

/** Шкала інтенсивності «менше → більше» з тих самих кошиків, що й клітинки карти. */
@Composable
private fun LegendScale(ramp: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.v3_heat_legend_less), color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        ramp.forEach { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(it)) }
        Text(stringResource(R.string.v3_heat_legend_more), color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

/**
 * `v3/RiskWindowCard`: час і шеврон «Чому це вікно?» (уся верхня частина натискається), мета-рядок 14sp, чіп
 * повторюваності поруч із крапками останніх 7 днів (крапки мають текстовий опис для TalkBack, не лише колір) і
 * нижній рядок лише для вимкненого чи призупиненого вікна. Вимкнене вікно — без прозорості всієї картки: контраст
 * тексту лишається, стан каже чіп «Вимкнено».
 */
@Composable
fun RiskWindowCard(window: RiskWindowUi, onOpenWhy: () -> Unit, modifier: Modifier = Modifier) {
    val primary = TeperaPalette.textPrimary
    val secondary = TeperaPalette.textSecondary
    val recurrence = stringResource(
        if (window.dayType == DayType.WEEKDAYS) R.string.v3_card_recurrence_weekdays else R.string.v3_card_recurrence_days,
        window.recurrenceDays, window.observedDays
    )
    val chip = when {
        window.isDisabled -> stringResource(R.string.v3_card_disabled_chip)
        window.isManual && window.observedDays == 0 -> stringResource(R.string.v3_card_manual)
        window.isManual -> stringResource(R.string.v3_card_manual_format, recurrence)
        else -> recurrence
    }
    val part = stringResource(partOfDayRes(window.partOfDay))
    val meta = if (window.appLabels.isEmpty()) {
        stringResource(R.string.v3_card_meta_no_apps, part, window.avgMinutes)
    } else {
        stringResource(R.string.v3_card_meta_format, part, window.appLabels.joinToString(", "), window.avgMinutes)
    }
    val applicable = window.dots.count { it != DotState.NOT_APPLICABLE }
    val dotsDescription = stringResource(R.string.v3_card_dots_cd, window.dots.count { it == DotState.HIT }, applicable)
    V3Card(modifier = modifier) {
        // Верхня частина відкриває «Чому це вікно?» (M6), нижня — план (L5) або кнопку «Скласти план».
        Column(
            Modifier.fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.v3_why_action), role = Role.Button, onClick = onOpenWhy),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatWindowTime(window.startMinute, window.endMinute),
                    color = if (window.isDisabled) secondary else primary,
                    fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(TeperaSymbols.ChevronRight, contentDescription = null, tint = secondary, modifier = Modifier.size(24.dp))
            }
            Text(meta, color = secondary, fontSize = 14.sp, lineHeight = 20.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TeperaChip(label = chip, compact = true, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.weight(1f))
                if (!window.isDisabled && window.dots.isNotEmpty()) {
                    Row(
                        Modifier.semantics(mergeDescendants = true) { contentDescription = dotsDescription },
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        window.dots.forEach { dot ->
                            Box(
                                Modifier.size(10.dp).clip(CircleShape).background(
                                    when (dot) {
                                        DotState.HIT -> TeperaPalette.heatmapAmber
                                        DotState.MISS -> TeperaPalette.heatmapNoDataFill
                                        DotState.NOT_APPLICABLE -> TeperaPalette.heatmapNoDataFill.copy(alpha = 0.4f)
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
        // Без плану: підказка «що зробити замість» приходить сама (оверлей і нагадування), тож картка лише про вікно.
        if (window.isDisabled || window.isPaused) {
            HorizontalDivider(color = TeperaPalette.borderDefault)
            Text(
                stringResource(if (window.isDisabled) R.string.v3_card_disabled else R.string.v3_card_paused),
                color = secondary, fontSize = 14.sp, lineHeight = 20.sp
            )
        }
    }
}

internal fun partOfDayRes(part: PartOfDay): Int = when (part) {
    PartOfDay.MORNING -> R.string.v3_part_morning
    PartOfDay.LUNCH -> R.string.v3_part_lunch
    PartOfDay.DAY -> R.string.v3_part_day
    PartOfDay.EVENING -> R.string.v3_part_evening
    PartOfDay.NIGHT -> R.string.v3_part_night
}
