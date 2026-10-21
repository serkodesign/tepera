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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.wrapContentWidth
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
                    val bars = remember(today, completed, todayOffline, todayOnline) { buildCalendarWeek(today, completed, todayOffline, todayOnline) }
                    GlassCard(cornerRadius = 24.dp) {
                        ChartTitle(stringResource(R.string.summary_week_online))
                        OnlineChart(bars, locale, weekdayLabels = true)
                    }
                    PeriodStatsRow(bars)
                }
                SummaryTab.MONTH -> {
                    val bars = remember(today, completed, todayOffline, todayOnline) { buildCalendarMonth(today, completed, todayOffline, todayOnline) }
                    GlassCard(cornerRadius = 24.dp) {
                        ChartTitle(monthName(today, locale))
                        OnlineChart(bars, locale, weekdayLabels = false)
                    }
                    PeriodStatsRow(bars)
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
            text = stringResource(R.string.summary_chart_subtitle_online),
            color = TeperaPalette.textSecondary,
            fontFamily = TeperaPalette.headlineFont,
            fontSize = 12.sp, // ≥ 12sp для змістовного тексту (CH-10); у Figma 11
            lineHeight = 13.2.sp
        )
    }
}

/** Тиждень / місяць: Online за період і в середньому на день (за завершені доби). */
@Composable
private fun PeriodStatsRow(bars: List<DayBar>) {
    val total = bars.mapNotNull { it.onlineMinutes }.takeIf { it.isNotEmpty() }?.sum()
    val average = averageOnline(bars)
    val totalText = if (total != null) heroDuration(total) else "—"
    val averageText = if (average != null) heroDuration(average) else "—"
    val longest = maxOf(totalText.length, averageText.length)
    val size = when {
        longest >= 12 -> 17
        longest >= 10 -> 20
        else -> 22
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(stringResource(R.string.summary_stat_online), totalText, size, Modifier.weight(1f))
        StatCard(stringResource(R.string.summary_stat_avg_online), averageText, size, Modifier.weight(1f))
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

/**
 * Стовпчики Online по днях зі шкалою годин зліва (і для тижня, і для місяця). Сьогодні — `card/active`, решта —
 * Online-колір; дні без даних — ледь помітна смужка. Тиждень підписує всі дні, місяць — кожен 7-й.
 */
@Composable
private fun OnlineChart(bars: List<DayBar>, locale: Locale, weekdayLabels: Boolean) {
    val online = TeperaPalette.onlineCard
    val todayColor = TeperaPalette.activityCardActive
    val grid = TeperaPalette.textSecondary.copy(alpha = 0.25f)
    val (top, step) = onlineAxis(bars.maxOfOrNull { it.onlineMinutes ?: 0 } ?: 0)
    val ticks = (0..top step step).toList()
    val plotHeight = 112.dp
    val labelHeight = 14.dp
    val hourSuffix = stringResource(R.string.summary_axis_hour_suffix)
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(plotHeight + labelHeight).width(40.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ticks.reversed().forEach { tick ->
                Box(Modifier.height(labelHeight), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = if (tick == 0) "0" else "$tick $hourSuffix",
                        color = TeperaPalette.textSecondary,
                        fontFamily = TeperaPalette.headlineFont,
                        fontSize = 12.sp,
                        lineHeight = 13.2.sp,
                        maxLines = 1
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(plotHeight + labelHeight)
                    .drawBehind {
                        val inset = labelHeight.toPx() / 2
                        val span = plotHeight.toPx()
                        ticks.forEach { tick ->
                            val y = inset + span * (1f - tick.toFloat() / top)
                            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                    }
                    .padding(vertical = labelHeight / 2),
                verticalAlignment = Alignment.Bottom
            ) {
                val gap = if (weekdayLabels) 6.dp else 1.5.dp
                bars.forEach { bar ->
                    val value = bar.onlineMinutes
                    val barHeight = if (value == null) 3.dp else maxOf(3f, plotHeight.value * value / (top * 60f)).dp
                    Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .padding(horizontal = gap)
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(if (weekdayLabels) 8.dp else 3.dp))
                                .background(
                                    when {
                                        value == null -> online.copy(alpha = 0.2f)
                                        bar.isToday -> todayColor
                                        else -> online
                                    }
                                )
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                bars.forEachIndexed { index, bar ->
                    val label = when {
                        weekdayLabels -> dayLabel(bar.date, locale)
                        index % 7 == 0 -> bar.date.dayOfMonth.toString()
                        else -> null
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (label != null) {
                            Text(
                                text = label,
                                modifier = Modifier.wrapContentWidth(unbounded = true),
                                color = TeperaPalette.textSecondary,
                                fontFamily = TeperaPalette.headlineFont,
                                fontSize = 12.sp,
                                lineHeight = 13.2.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
