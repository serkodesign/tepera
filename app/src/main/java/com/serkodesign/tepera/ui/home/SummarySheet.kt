package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.pattern.HourlyHeatGridTall
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaPalette
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Шторка підсумків (docs/HANDOFF.md, розділ 8; Figma `SummarySheet` 679:12925): вкладки Вчора / Тиждень / Місяць,
 * вміст за вкладкою, два числа "без телефону" / "online" і CTA "Відкрити в статистиці". Закривається жестом вниз
 * або тапом по скриму. Значення — ті самі, що в тізерах ([SummaryModel]); кольори — лише семантичні ролі.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarySheet(
    initialTab: SummaryTab,
    today: LocalDate,
    completed: Map<LocalDate, DayStat>,
    todayOffline: Int?,
    todayOnline: Int?,
    yesterdayHourlyMinutes: List<Int>?,
    onOpenStats: () -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(initialTab) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TeperaPalette.colors.backgroundTop ?: Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TeperaPalette.textSecondary.copy(alpha = 0.5f))
            )
        }
    ) {
        com.serkodesign.tepera.util.ProvideAppLocale {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SummaryTabs(selected = tab, onSelect = { tab = it })

            val yesterday = completed[today.minusDays(1)]
            when (tab) {
                SummaryTab.YESTERDAY -> {
                    GlassCard(cornerRadius = 16.dp) {
                        Text(
                            text = stringResource(R.string.pattern_card_title),
                            modifier = Modifier.semantics { heading() },
                            color = TeperaPalette.buttonBrandDark,
                            fontFamily = TeperaPalette.headlineFont,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                        HourlyHeatGridTall(hourlyMinutes = yesterdayHourlyMinutes, modifier = Modifier.height(190.dp))
                    }
                    StatsRow(offline = yesterday?.offlineMinutes, online = yesterday?.onlineMinutes)
                }
                SummaryTab.WEEK -> {
                    val bars = remember(today, completed, todayOffline) { buildCalendarWeek(today, completed, todayOffline) }
                    GlassCard(cornerRadius = 24.dp) {
                        ChartTitle(stringResource(R.string.summary_week))
                        WeekChart(bars, locale)
                    }
                    val totals = remember(bars, completed, todayOnline) { sumTotals(bars, completed, todayOnline) }
                    StatsRow(offline = totals.offlineMinutes.takeIf { bars.any { it.offlineMinutes != null } }, online = totals.onlineMinutes.takeIf { bars.any { it.offlineMinutes != null } })
                }
                SummaryTab.MONTH -> {
                    val bars = remember(today, completed, todayOffline) { buildCalendarMonth(today, completed, todayOffline) }
                    GlassCard(cornerRadius = 24.dp) {
                        ChartTitle(monthName(today, locale))
                        MonthGrid(today, bars, locale)
                    }
                    val totals = remember(bars, completed, todayOnline) { sumTotals(bars, completed, todayOnline) }
                    StatsRow(offline = totals.offlineMinutes.takeIf { bars.any { it.offlineMinutes != null } }, online = totals.onlineMinutes.takeIf { bars.any { it.offlineMinutes != null } })
                }
            }

            TeperaButton(
                text = stringResource(R.string.summary_open_stats),
                onClick = onOpenStats,
                modifier = Modifier.fillMaxWidth()
            )
        }
        }
    }
}

@Composable
private fun SummaryTabs(selected: SummaryTab, onSelect: (SummaryTab) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val colors = TeperaPalette.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (colors.isDark) colors.listItem else Color(0x80FFFFFF))
            .then(TeperaPalette.activityCardIdleBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            SummaryTab.YESTERDAY to R.string.summary_yesterday,
            SummaryTab.WEEK to R.string.summary_week,
            SummaryTab.MONTH to R.string.summary_month
        ).forEach { (tab, label) ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) TeperaPalette.activityCardActive else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(label),
                    color = if (isSelected) TeperaPalette.onPrimary else TeperaPalette.textSecondary,
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 15.4.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** Скляна картка шторки (`surface/glass` + `card/activity-border`). */
@Composable
private fun GlassCard(cornerRadius: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TeperaPalette.activityCardIdle)
            .then(TeperaPalette.activityCardIdleBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun ChartTitle(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = TeperaPalette.textPrimary,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 19.8.sp
        )
        Text(
            text = stringResource(R.string.summary_chart_subtitle),
            color = TeperaPalette.textSecondary,
            fontFamily = TeperaPalette.headlineFont,
            fontSize = 12.sp, // ≥ 12sp для змістовного тексту (CH-10); у Figma 11
            lineHeight = 13.2.sp
        )
    }
}

/** Два числа: "без телефону" і "online". null — немає даних ("—"). */
@Composable
private fun StatsRow(offline: Int?, online: Int?) {
    // Однаковий кегль у двох картках: визначається довшим із двох значень (інакше картки різної висоти).
    val offlineText = if (offline != null) heroDuration(offline) else "—"
    val onlineText = if (online != null) heroDuration(online) else "—"
    val longest = maxOf(offlineText.length, onlineText.length)
    val size = when {
        longest >= 12 -> 17 // "168 год 59 хв" у півширині — зменшуємо замість обрізання
        longest >= 10 -> 20
        else -> 22
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(stringResource(R.string.hero_orb_label), offlineText, size, Modifier.weight(1f))
        StatCard(stringResource(R.string.summary_stat_online), onlineText, size, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, text: String, size: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(TeperaPalette.activityCardIdle)
            .then(TeperaPalette.activityCardIdleBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = TeperaPalette.textSecondary,
            fontFamily = TeperaPalette.headlineFont,
            fontSize = 12.sp,
            lineHeight = 13.2.sp
        )
        Text(
            text = text,
            color = TeperaPalette.textPrimary,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = size.sp,
            lineHeight = (size * 1.1f).sp,
            maxLines = 1
        )
    }
}

private fun dayLabel(date: LocalDate, locale: Locale): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }

private fun monthName(today: LocalDate, locale: Locale): String =
    Month.of(today.monthValue).getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

/** Тиждень: 7 стовпчиків пн–нд (радіус 10, ширина 32), сьогодні — `card/active`, решта — `data/offline-life` @80%. */
@Composable
private fun WeekChart(bars: List<DayBar>, locale: Locale) {
    val base = TeperaPalette.restOfDayCard
    val today = TeperaPalette.activityCardActive
    val max = maxOf(60, bars.maxOfOrNull { it.offlineMinutes ?: 0 } ?: 0).toFloat()
    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        bars.forEach { bar ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.height(112.dp).width(32.dp), contentAlignment = Alignment.BottomCenter) {
                    val value = bar.offlineMinutes
                    val barHeight = if (value == null) 4.dp else maxOf(4f, 112f * value / max).dp
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    value == null -> base.copy(alpha = 0.25f)
                                    bar.isToday -> today
                                    else -> base.copy(alpha = 0.8f)
                                }
                            )
                    )
                }
                Text(
                    text = dayLabel(bar.date, locale),
                    modifier = Modifier.padding(top = 8.dp),
                    color = TeperaPalette.textSecondary,
                    fontFamily = TeperaPalette.headlineFont,
                    fontSize = 12.sp,
                    lineHeight = 13.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** Місяць: календарна сітка (пн–нд), клітинка — інтенсивність "без телефону" (`data/offline-life`), сьогодні — `card/active`. */
@Composable
private fun MonthGrid(today: LocalDate, bars: List<DayBar>, locale: Locale) {
    val base = TeperaPalette.restOfDayCard
    val todayColor = TeperaPalette.activityCardActive
    val empty = TeperaPalette.textPrimary.copy(alpha = 0.06f)
    val max = maxOf(60, bars.maxOfOrNull { it.offlineMinutes ?: 0 } ?: 0).toFloat()
    val offset = YearMonth.from(today).atDay(1).dayOfWeek.value - 1
    val cells: List<DayBar?> = List(offset) { null } + bars
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            (0 until 7).forEach { i ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = dayLabel(LocalDate.of(2026, 10, 12).plusDays(i.toLong()), locale), // будь-який понеділок + i
                        color = TeperaPalette.textSecondary,
                        fontFamily = TeperaPalette.headlineFont,
                        fontSize = 12.sp,
                        lineHeight = 13.2.sp,
                        maxLines = 1
                    )
                }
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (0 until 7).forEach { i ->
                    val bar = week.getOrNull(i)
                    val value = bar?.offlineMinutes
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    bar == null -> Color.Transparent
                                    bar.isToday -> todayColor
                                    value == null -> empty
                                    else -> base.copy(alpha = 0.35f + 0.55f * (value / max).coerceIn(0f, 1f))
                                }
                            )
                    )
                }
            }
        }
    }
}
