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
import androidx.compose.material3.MaterialTheme
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
import com.serkodesign.tepera.ui.theme.M3Card
import com.serkodesign.tepera.ui.theme.M3Extra
import com.serkodesign.tepera.ui.theme.TeperaExtraShapes
import com.serkodesign.tepera.ui.theme.TeperaSpacing
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
    // Дизайн-система v3.1 (3.8 плану): клітинки dataHeat0–5 (форма 2dp), пунктир «години ризику» — primary, осі й
    // легенда onSurfaceVariant, контейнер M3Card (поле 16dp, проміжки 8dp). Кошики HeatGrid 0–4 → рівні 0–5 через
    // HeatLevels (стара прозорість бурштину → рівень, таблиця 2.2). Підписи осей — bodySmall 12sp, а не labelSmall 11sp
    // зі специфікації: рішення проєкту CH-10 «змістовний текст ≥ 12sp» (години на осі — зміст).
    val extra = M3Extra.colors
    val cellColors = List(5) { bucket -> extra.heat(HeatLevels.levelForBucket(bucket)) }
    val dashColor = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val axisStyle = MaterialTheme.typography.bodySmall
    val rows = dates.size

    M3Card(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        spacing = TeperaSpacing.s
    ) {
        Row {
            Column(Modifier.width(28.dp)) {
                dates.forEach { date ->
                    Box(Modifier.height(16.dp)) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase() },
                            color = secondary, style = axisStyle, maxLines = 1
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
                        color = cellColors[bucket],
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
                    color = secondary, style = axisStyle, maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(labelWidth).offset(x = (center - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth))
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendItem(stringResource(R.string.v3_heat_legend_feeds)) {
                Box(Modifier.size(10.dp).clip(TeperaExtraShapes.heatCell).background(extra.signalVivid))
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
        LegendScale(HeatLevels.USED_LEVELS.map { extra.heat(it) })
    }
}

@Composable
private fun LegendItem(label: String, marker: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        marker()
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

/** Шкала інтенсивності «менше → більше» з тих самих рівнів, що й клітинки карти (лише вживані кошиками). */
@Composable
private fun LegendScale(ramp: List<Color>) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.v3_heat_legend_less), color = secondary, style = MaterialTheme.typography.bodySmall)
        ramp.forEach { Box(Modifier.size(10.dp).clip(TeperaExtraShapes.heatCell).background(it)) }
        Text(stringResource(R.string.v3_heat_legend_more), color = secondary, style = MaterialTheme.typography.bodySmall)
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
    val primary = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
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
    // Дизайн-система v3.1 (3.3 плану): M3Card, час titleLarge + бейдж повторюваності праворуч, мета bodySmall і крапки
    // через 4dp. Блок «план / немає плану» зі специфікації не додано: у RiskWindowUi даних про план нема, а картка
    // свідомо про саме вікно (підказка приходить через оверлей і нагадування) — нова функція поза обсягом міграції.
    M3Card(modifier = modifier) {
        // Верхня частина відкриває «Чому це вікно?» (M6).
        Column(
            Modifier.fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.v3_why_action), role = Role.Button, onClick = onOpenWhy),
            verticalArrangement = Arrangement.spacedBy(TeperaSpacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TeperaSpacing.s)) {
                Text(
                    text = formatWindowTime(window.startMinute, window.endMinute),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (window.isDisabled) secondary else primary
                )
                // Пауза — тег одразу після годин; зняти її можна лише з «Чому це вікно?».
                if (window.isPaused && !window.isDisabled) M3RecurrenceBadge(stringResource(R.string.v3_card_paused))
                Spacer(Modifier.weight(1f))
                Icon(TeperaSymbols.ChevronRight, contentDescription = null, tint = secondary, modifier = Modifier.size(24.dp))
            }
            Text(meta, style = MaterialTheme.typography.bodySmall, color = secondary)
            // Бейдж повторюваності — в одному рядку з крапками, а не поруч із часом (специфікація 3.3): картки каруселі
            // вузькі (~260dp), і в одному ряді з часом і шевроном бейдж обрізається. Без FlowRow: карусель вирівнює
            // висоту через IntrinsicSize.Max, а FlowRow дає для нього хибну висоту (картка обрізалась знизу).
            Row(
                Modifier.fillMaxWidth().padding(top = TeperaSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Без Spacer із вагою: він ділив би вільне місце навпіл із бейджем і обрізав його («4 з 7 днів» → «4 з 7»).
                M3RecurrenceBadge(chip, modifier = Modifier.weight(1f, fill = false).padding(end = TeperaSpacing.m))
                if (!window.isDisabled && window.dots.isNotEmpty()) M3RecurrenceDots(window.dots, dotsDescription)
            }
        }
        if (window.isDisabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(R.string.v3_card_disabled), style = MaterialTheme.typography.bodyMedium, color = secondary)
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
