package com.serkodesign.tepera.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaChip
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Хронологія: порожні клітинки — ледь помітний темний відтінок, "до початку дня" — ще тихіший.
private val SlotBlank = Color(0x14003926)
private val SlotBeforeStart = Color(0x08003926)
// Пауза без телефону — глибший відтінок зеленого за "Офлайн-життя" (#C5E2CB), бо пауза — його підвид.
private val SlotPause = Color(0xFF6DBF94)

/**
 * Статистика → День (вчора): чипи меж дня, хронологія доби, паузи, тихий рядок порівняння з власною
 * типовою добою. Усе — нейтральні факти (принцип "застосунок не оцінює"): жодних слів-оцінок,
 * traffic-light кольорів чи порівняння з нормою.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayDetailsSection(
    details: DayDetailsUiState,
    hasUsageAccess: Boolean,
    onOpenUsageAccessSettings: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Без доступу до статистики використання немає ні Online, ні пауз — чесний заклик, як в інших картках.
    if (!hasUsageAccess) {
        TeperaCard {
            Text(stringResource(R.string.usage_access_prompt_title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.usage_access_prompt_body), style = MaterialTheme.typography.bodyMedium)
            TeperaButton(
                text = stringResource(R.string.usage_access_open_settings),
                onClick = onOpenUsageAccessSettings,
                type = TeperaButtonType.Primary
            )
        }
    }

    // Межі дня й розблокування — "деталі дня" (T-14/T-10): лише тут і в Щоденнику, не на Home.
    if (details.firstUseMillis != null || details.lastUseMillis != null || details.unlockCount != null) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            details.firstUseMillis?.let {
                TeperaChip(stringResource(R.string.home_card_first_unlock_label), value = timeFormat.format(Date(it)))
            }
            details.lastUseMillis?.let {
                TeperaChip(stringResource(R.string.diary_last_phone_use_yesterday_label), value = timeFormat.format(Date(it)))
            }
            details.unlockCount?.let {
                TeperaChip(stringResource(R.string.diary_unlock_yesterday_label), value = it.toString())
            }
        }
    }

    if (details.hasTimelineData) DayTimelineCard(details)

    details.pauses?.let { PausesCard(it, timeFormat) }
}

/** Смуга 48 клітинок по 30 хв (00:00-24:00) + позначки годин + легенда лише того, що є в добі. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayTimelineCard(details: DayDetailsUiState) {
    val timeline = details.timeline
    val presentCategories = timeline.filterIsInstance<TimelineSlot.Category>()
        .map { it.categoryId }.distinct().mapNotNull { details.categoriesById[it] }

    TeperaCard(title = stringResource(R.string.stats_day_timeline_title)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            timeline.forEach { slot ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(slotColor(slot, details))
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f))
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (timeline.any { it is TimelineSlot.Online }) {
                LegendItem(TeperaPalette.onlineCard, stringResource(R.string.balance_online_label))
            }
            presentCategories.forEach { LegendItem(categoryColor(it.colorHex), categoryDisplayName(it)) }
            if (timeline.any { it is TimelineSlot.Offline }) {
                LegendItem(TeperaPalette.restOfDayCard, stringResource(R.string.stats_day_legend_offline))
            }
            if (timeline.any { it is TimelineSlot.Pause }) {
                LegendItem(SlotPause, stringResource(R.string.stats_day_legend_pause))
            }
        }

        // Тихий рядок порівняння зі своєю типовою добою — частина картки доби, а не окремий текст.
        details.comparison?.let { c ->
            Text(
                text = stringResource(
                    R.string.stats_day_compare,
                    durationText(c.yesterdayMinutes), c.daysCount, durationText(c.typicalMinutes)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TeperaPalette.buttonBrandDark
            )
        }
    }
}

private fun slotColor(slot: TimelineSlot, details: DayDetailsUiState): Color = when (slot) {
    is TimelineSlot.Category ->
        details.categoriesById[slot.categoryId]?.let { categoryColor(it.colorHex) } ?: SlotBlank
    is TimelineSlot.Online -> TeperaPalette.onlineCard
    is TimelineSlot.Offline -> TeperaPalette.restOfDayCard
    is TimelineSlot.Pause -> SlotPause
    is TimelineSlot.BeforeStart -> SlotBeforeStart
    is TimelineSlot.Blank -> SlotBlank
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TeperaPalette.buttonBrandDark)
    }
}

@Composable
private fun PausesCard(summary: PauseSummary, timeFormat: SimpleDateFormat) {
    val start = summary.longest.startTime
    val end = start + summary.longest.durationMinutes * 60_000L
    val range = "${timeFormat.format(Date(start))}–${timeFormat.format(Date(end))}"
    val longestLine = stringResource(
        R.string.stats_day_pauses_longest,
        durationText(summary.longest.durationMinutes), range
    ) + (summary.longestCategory?.let { " · " + categoryDisplayName(it) } ?: "")

    TeperaCard(title = stringResource(R.string.stats_day_pauses_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.stats_day_pauses_count, summary.count),
                style = MaterialTheme.typography.bodyMedium,
                color = TeperaPalette.buttonBrandDark
            )
            Text(longestLine, style = MaterialTheme.typography.bodyMedium, color = TeperaPalette.buttonBrandDark)
        }
    }
}

/** "2 год 10 хв" / "45 хв" / "3 год" — точні хвилини (не заокруглення до чверті, як у записах). */
@Composable
fun durationText(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, rest)
        rest == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, rest)
    }
}
