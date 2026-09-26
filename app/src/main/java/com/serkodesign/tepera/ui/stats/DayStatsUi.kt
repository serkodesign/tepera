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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryGlyphColor
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.pattern.PatternUiState
import com.serkodesign.tepera.ui.theme.StatTile
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaChip
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Хронологія: порожні клітинки — ледь помітний темний відтінок, "до початку дня" — ще тихіший.
private val SlotBlank = Color(0x14003926)
private val SlotBeforeStart = Color(0x08003926)
// Пауза без телефону — глибший відтінок зеленого за "Офлайн-життя" (#C5E2CB), бо пауза — його підвид.
private val SlotPause = Color(0xFF6DBF94)

/**
 * Статистика → День (вчора): картки меж дня, тепловий патерн, хронологія доби, паузи, тихий
 * рядок порівняння з власною типовою добою. Усе — нейтральні факти (принцип "застосунок не
 * оцінює"): жодних слів-оцінок, traffic-light кольорів чи порівняння з нормою.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayDetailsSection(
    details: DayDetailsUiState,
    hasUsageAccess: Boolean,
    onOpenUsageAccessSettings: () -> Unit,
    patternState: PatternUiState
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
                type = TeperaButtonType.Secondary
            )
        }
    }

    // Межі дня й розблокування — "деталі дня" (T-14/T-10): лише тут і в Щоденнику, не на Home.
    // За прямим запитом користувача — три картки в ряд (`StatTile`), не чипи в FlowRow.
    if (details.firstUseMillis != null || details.lastUseMillis != null || details.unlockCount != null) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            details.firstUseMillis?.let {
                StatTile(
                    label = stringResource(R.string.home_card_first_unlock_label),
                    value = timeFormat.format(Date(it)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            details.lastUseMillis?.let {
                StatTile(
                    label = stringResource(R.string.diary_last_phone_use_yesterday_label),
                    value = timeFormat.format(Date(it)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            details.unlockCount?.let {
                StatTile(
                    label = stringResource(R.string.diary_unlock_yesterday_label),
                    value = it.toString(),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }

    // За прямим запитом користувача: тепловий патерн переїхав одразу під картки меж дня — вище
    // хронології/пауз/порівняння, які раніше йшли одразу за чипами.
    PatternCard(state = patternState, period = StatsPeriod.DAY)

    // GAP-9: у день встановлення "вчора" ще не існує — один спокійний рядок замість порожнього екрана чи нулів
    // (розділ 4 SRS: без докору, "Поки порожньо", не "Ти нічого не зафіксував").
    val nothingYet = details.firstUseMillis == null && details.lastUseMillis == null &&
        details.unlockCount == null && !details.hasTimelineData && details.pauses == null
    if (hasUsageAccess && nothingYet) {
        TeperaCard {
            Text(
                stringResource(R.string.stats_day_first_day_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
            )
        }
    }

    if (details.hasTimelineData) DayTimelineCard(details)

    details.pauses?.let { PausesCard(it, timeFormat) }
}

/**
 * Картка "Вчорашня доба" — організована за M3 (картка з заголовком і допоміжним текстом, секції через
 * розділювач, крок 8/16dp): (1) заголовок + дата вчорашнього дня; (2) хронологія — пігулка з 48 клітинок
 * по 30 хв із підписами 00/06/12/18/24; (3) легенда — рівні "чіпи" (кружок + назва, лише те, що є в добі),
 * що переносяться рядками, а не рваний рядок тексту; (4) через розділювач — порівняння з власною типовою
 * добою ([OnlineComparisonBar]), якщо є достатньо днів історії.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayTimelineCard(details: DayDetailsUiState) {
    val timeline = details.timeline
    val presentCategories = timeline.filterIsInstance<TimelineSlot.Category>()
        .map { it.categoryId }.distinct().mapNotNull { details.categoriesById[it] }
    val locale = LocalConfiguration.current.locales[0]
    val dateText = remember(details.dayStartMillis, locale) {
        SimpleDateFormat("EEEE, d MMMM", locale).format(Date(details.dayStartMillis)).replaceFirstChar { it.titlecase(locale) }
    }

    TeperaCard(title = stringResource(R.string.stats_day_timeline_title), subtitle = dateText) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(100.dp)),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                timeline.forEach { slot ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().background(slotColor(slot, details))
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "06", "12", "18", "24").forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TeperaPalette.buttonBrandDark.copy(alpha = 0.75f))
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timeline.any { it is TimelineSlot.Online }) {
                LegendChip(TeperaPalette.onlineCard, stringResource(R.string.balance_online_label))
            }
            presentCategories.forEach { LegendChip(categoryColor(it.colorHex), categoryDisplayName(it)) }
            if (timeline.any { it is TimelineSlot.Offline }) {
                LegendChip(TeperaPalette.restOfDayCard, stringResource(R.string.stats_day_legend_offline))
            }
            if (timeline.any { it is TimelineSlot.Pause }) {
                LegendChip(SlotPause, stringResource(R.string.stats_day_legend_pause))
            }
        }

        // Порівняння зі своєю типовою добою — окрема секція під розділювачем (за прямим запитом користувача:
        // графічно, а не голий рядок тексту).
        details.comparison?.let { comparison ->
            SectionDivider()
            OnlineComparisonBar(comparison)
        }
    }
}

/** Тонкий розділювач секцій картки (M3 divider: 1dp, приглушений колір тексту). */
@Composable
private fun SectionDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(TeperaPalette.buttonBrandDark.copy(alpha = 0.12f)))
}

/**
 * Порівняння Online вчора зі своєю типовою добою — шкала й два рядки "ключ — значення" (як таблиця, а не два
 * підписи по краях): заповнена частина шкали — Online вчора (колір Online скрізь у застосунку), тиха
 * вертикальна позначка — типова доба (медіана за N днів). Той самий принцип, що позначка орієнтиру на Home
 * (FR-3.10 — тонка лінія БЕЗ підпису на самій шкалі, ніколи не змінює колір, жодного "більше/менше" в
 * кольорі); пояснення позначки — у рядку легенди нижче з такою самою позначкою. Числа точні (FR-P.6).
 */
@Composable
private fun OnlineComparisonBar(comparison: OnlineComparison) {
    val maxMinutes = maxOf(comparison.yesterdayMinutes, comparison.typicalMinutes, 1)
    val yesterdayFraction = (comparison.yesterdayMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
    val typicalFraction = (comparison.typicalMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(TeperaPalette.restOfDayCard),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(yesterdayFraction.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(TeperaPalette.onlineCard)
                )
                Box(modifier = Modifier.weight((1f - yesterdayFraction).coerceAtLeast(0.001f)).fillMaxHeight())
            }
            Box(
                modifier = Modifier
                    .comparisonMarkerAt(typicalFraction)
                    .width(2.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(TeperaPalette.buttonBrandDark.copy(alpha = 0.5f))
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ComparisonRow(
                marker = { Box(Modifier.size(12.dp).clip(CircleShape).background(TeperaPalette.onlineCard)) },
                label = stringResource(R.string.stats_day_compare_yesterday),
                value = durationText(comparison.yesterdayMinutes),
                emphasized = true
            )
            ComparisonRow(
                marker = {
                    Box(Modifier.width(12.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(2.dp).height(12.dp).clip(RoundedCornerShape(1.dp)).background(TeperaPalette.buttonBrandDark.copy(alpha = 0.5f)))
                    }
                },
                label = stringResource(R.string.stats_day_compare_typical, comparison.daysCount),
                value = durationText(comparison.typicalMinutes),
                emphasized = false
            )
        }
    }
}

/** Рядок "маркер · підпис · значення" з вирівнюванням значення праворуч. */
@Composable
private fun ComparisonRow(marker: @Composable () -> Unit, label: String, value: String, emphasized: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        marker()
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = TeperaPalette.buttonBrandDark.copy(alpha = if (emphasized) 1f else 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TeperaPalette.buttonBrandDark
        )
    }
}

/** Позиціонує елемент на [fraction] ширини батька, центрований — той самий прийом, що позначка орієнтиру на Home. */
private fun Modifier.comparisonMarkerAt(fraction: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = constraints.maxWidth
    layout(width, placeable.height) {
        val x = (width * fraction).roundToInt() - placeable.width / 2
        placeable.placeRelative(x.coerceIn(0, (width - placeable.width).coerceAtLeast(0)), 0)
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

/**
 * Елемент легенди — тональний "чіп" (M3): кружок кольору 12dp + назва 14sp на приглушеному фоні, висота 32dp.
 * Однакові чіпи переносяться рядками акуратніше, ніж вільний рядок тексту з кольоровими квадратами.
 */
@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(TeperaPalette.buttonBrandDark.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TeperaPalette.buttonBrandDark)
    }
}

/**
 * Картка "Паузи без телефону" (за прямим запитом користувача, редизайн): замість двох рядків
 * голого тексту — чипи (кількість/найдовша, той самий `TeperaChip`, що решта Статистики) + тонка
 * 24-годинна вісь-"інфографіка" ([PausePositionBar]), що показує, КОЛИ саме в добі сталась
 * найдовша пауза (не лише скільки), і кольоровий бейдж категорії (той самий стиль 40dp-кружка
 * з `CategoryPickerDialog`/рядків Щоденника, лише менший — тут другорядна деталь, не головний
 * елемент рядка), якщо паузу позначили.
 */
@Composable
private fun PausesCard(summary: PauseSummary, timeFormat: SimpleDateFormat) {
    val start = summary.longest.startTime
    val end = start + summary.longest.durationMinutes * 60_000L
    val range = "${timeFormat.format(Date(start))}–${timeFormat.format(Date(end))}"
    val category = summary.longestCategory

    TeperaCard(title = stringResource(R.string.stats_day_pauses_title)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TeperaChip(
                label = stringResource(R.string.stats_day_pauses_count_chip_label),
                value = summary.count.toString()
            )
            TeperaChip(
                label = stringResource(R.string.stats_day_pauses_longest_chip_label),
                value = durationText(summary.longest.durationMinutes)
            )
        }

        PausePositionBar(startMillis = start, endMillis = end)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val badgeColor = category?.let { categoryColor(it.colorHex) } ?: SlotPause
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category?.let { categoryIcon(it.iconName) } ?: TeperaSymbols.PhonelinkOff,
                    contentDescription = null,
                    tint = categoryGlyphColor(badgeColor),
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(range, style = MaterialTheme.typography.bodyMedium, color = TeperaPalette.buttonBrandDark)
                category?.let {
                    Text(
                        categoryDisplayName(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Тонка вісь 00:00-24:00 із підсвіченим сегментом рівно там, де в добі сталась найдовша пауза —
 * та сама ідея, що хронологія доби вище, лише звужена до одного факту "коли". Трек — `SlotBlank`
 * (той самий нейтральний відтінок, що порожні клітинки хронології, тож обидва елементи картки
 * читаються як одна візуальна мова), підсвічений сегмент — `SlotPause`. Мінімальна вага сегмента
 * (0.008f ≈ 12 хв доби) — щоб навіть коротка (полюс мінімуму FR-D.1, 30 хв) пауза лишалась
 * видимою смужкою, а не зникала в заокругленні пікселя.
 */
@Composable
private fun PausePositionBar(startMillis: Long, endMillis: Long) {
    val calendar = remember { Calendar.getInstance() }
    fun fractionOfDay(millis: Long): Float {
        calendar.timeInMillis = millis
        val minutesOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return (minutesOfDay / 1440f).coerceIn(0f, 1f)
    }

    val startFraction = fractionOfDay(startMillis)
    val endFractionRaw = fractionOfDay(endMillis)
    // Пауза, що перетинає північ (напр. 23:50-00:20): "кінець" за годинником менший за "початок" —
    // трактуємо як таку, що триває до кінця цієї доби (наступна доба — вже інша картка).
    val endFraction = if (endFractionRaw <= startFraction) 1f else endFractionRaw

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(SlotBlank)
        ) {
            Box(modifier = Modifier.weight(startFraction.coerceAtLeast(0.001f)).fillMaxHeight())
            Box(
                modifier = Modifier
                    .weight((endFraction - startFraction).coerceAtLeast(0.008f))
                    .fillMaxHeight()
                    .background(SlotPause)
            )
            Box(modifier = Modifier.weight((1f - endFraction).coerceAtLeast(0.001f)).fillMaxHeight())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TeperaPalette.buttonBrandDark.copy(alpha = 0.5f))
            }
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
