package com.serkodesign.tepera.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour
import kotlin.math.roundToInt
import java.time.Instant
import java.time.ZoneId

/**
 * FR-3.1–3.12, FR-5.1 (SRS v2.5): "Мій день" — перша сторінка горизонтального пейджера Home
 * (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 192:726, "My day"). Замість заголовка "Твій
 * день: X" (FR-3.7, росте разом з реальним часом) — дві білі плашки: "Перше розблокування HH:MM" і
 * "День триває X" (той самий FR-3.7-показник). Далі тиха багатосегментна шкала (Online + кожна
 * залогована сьогодні категорія своїм кольором + нейтральна "Решта дня", суцільна смуга без
 * проміжків, біла "доріжка" для ще не прожитого часу) з тихою засічкою орієнтиру БЕЗ підпису
 * (FR-3.10) і трикутниками-вказівниками "зараз" зверху й знизу, та легенда: квадрат 8dp + назва +
 * час у форматі `Г:ХХ`, без відсотків (FR-P.6). Свідомо НЕ протиставлення Online/Offline на одній
 * шкалі (FR-3.8 — різні джерела, різна природа підрахунку).
 *
 * Заголовка й ⓘ у стані "доступ є" більше нема (макет їх не містить); fallback без доступу лишає
 * власне пояснення з кнопкою "Чому це потрібно?".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyDayCard(
    state: BalanceUiState,
    onOpenUsageAccessSettings: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeCardSurface(modifier = modifier, containerColor = TeperaPalette.homeCardFillMyDay) {
        when (state.hasUsageAccess) {
            null -> Unit // перевірка ще триває, картка мовчить, щоб не блимати fallback-текстом
            false -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.usage_access_prompt_title), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.usage_access_prompt_body), style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TeperaButton(
                                text = stringResource(R.string.usage_access_learn_more),
                                onClick = onLearnMore,
                                size = TeperaButtonSize.Medium,
                                type = TeperaButtonType.Tertiary
                            )
                            TeperaButton(
                                text = stringResource(R.string.usage_access_open_settings),
                                onClick = onOpenUsageAccessSettings,
                                size = TeperaButtonSize.Medium,
                                type = TeperaButtonType.Primary
                            )
                        }
                    }
                }
            }
            true -> {
                val segments = daySegments(state)
                if (segments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // FlowRow: на вузьких екранах (напр. 360dp) плашки переносяться, а не обрізаються.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (state.dayStartMillis > 0L) {
                                HomeLabelValueChip(
                                    label = stringResource(R.string.home_card_first_unlock_label),
                                    value = formatClock(state.dayStartMillis)
                                )
                            }
                            HomeLabelValueChip(
                                label = stringResource(R.string.home_card_day_last_label),
                                value = formatBalanceDuration(state.dayLengthMinutes)
                            )
                        }
                        DayStructureBar(
                            segments = segments,
                            targetMinutes = state.targetMinutes,
                            daySpanMinutes = state.daySpanMinutes,
                            dayLengthMinutes = state.dayLengthMinutes
                        )
                    }
                    DayStructureLegend(segments = segments)
                }
            }
        }
    }
}

private data class DaySegment(val label: String, val color: Color, val minutes: Int)

/**
 * FR-3.3, FR-3.4: порядок — Online, потім кожна категорія з ненульовим часом сьогодні (своїм
 * кольором), потім нейтральна "Решта дня" останньою (референсний макет, розділ 4.4).
 */
@Composable
private fun daySegments(state: BalanceUiState): List<DaySegment> {
    val onlineLabel = stringResource(R.string.balance_online_label)
    val restLabel = stringResource(R.string.balance_rest_of_day_label)
    return buildList {
        if (state.onlineMinutes > 0) add(DaySegment(onlineLabel, TeperaPalette.onlineCard, state.onlineMinutes))
        state.categorySegments.forEach {
            add(DaySegment(categoryDisplayName(it.category), categoryColor(it.category.colorHex), it.minutes))
        }
        if (state.restOfDayMinutes > 0) add(DaySegment(restLabel, TeperaPalette.restOfDayCard, state.restOfDayMinutes))
    }
}

@Composable
private fun DayStructureBar(
    segments: List<DaySegment>,
    targetMinutes: Int,
    daySpanMinutes: Int,
    dayLengthMinutes: Int
) {
    // FR-3.10: тиха засічка орієнтиру — тонка вертикальна лінія, БЕЗ підпису, ніколи не
    // змінює колір при перевищенні (розділ 4.3–4.4 SRS: "якщо з'явиться спокуса підсвітити
    // перевищення кольором — це сигнал звірити рішення з розділом 4, не з інтуїцією"). Позиція —
    // частка від повного діапазону шкали (пробудження → 00:00, за запитом користувача), а не
    // лише від довжини дня, що минула, — інакше засічка "стрибала" б праворуч разом з ростом дня.
    val referenceMinutes = maxOf(daySpanMinutes, targetMinutes, 1)
    val markerFraction = (targetMinutes.toFloat() / referenceMinutes).coerceIn(0f, 1f)

    // Позначка "Now" — де саме "зараз" на шкалі "пробудження → 00:00". На відміну від засічки
    // орієнтиру вище, ця позначка РУХАЄТЬСЯ разом із часом — по своїй природі не евалюативна
    // (просто "де ми на годиннику", не оцінка), тож лишається трикутниками-вказівниками: ▼ зверху
    // й ▲ знизу шкали (Figma node 192:726).
    val nowFraction = (dayLengthMinutes.toFloat() / daySpanMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)

    // Без BoxWithConstraints: він робить субкомпозицію на кожному перевимірі, а слайдер Home
    // перевимірює сторінки на кожному кадрі свайпу — на Huawei P9 це давало 63% рваних кадрів.
    // Позиції маркерів рахує легкий layout-модифікатор [atFraction] (без субкомпозиції).
    Column(Modifier.fillMaxWidth()) {
        NowPointer("▼", nowFraction)
        Box(Modifier.fillMaxWidth()) {
            // Біла "доріжка" (Figma): те, що ще не сталося (від "Now" до півночі), лишається
            // незафарбованим — "Офлайн-життя" заповнює лише до позначки "Now".
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            ) {
                segments.forEach { segment ->
                    Box(
                        modifier = Modifier
                            .weight(segment.minutes.coerceAtLeast(1).toFloat())
                            .fillMaxHeight()
                            .background(segment.color)
                    )
                }
                val futureMinutes = (daySpanMinutes - dayLengthMinutes).coerceAtLeast(0)
                if (futureMinutes > 0) {
                    Box(modifier = Modifier.weight(futureMinutes.toFloat()).fillMaxHeight())
                }
            }
            Box(
                modifier = Modifier
                    .atFraction(markerFraction, centered = false)
                    .width(1.dp)
                    .height(16.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
        NowPointer("▲", nowFraction)
    }
}

/** Ставить елемент на [fraction] ширини батька (без субкомпозиції, на відміну від BoxWithConstraints). */
private fun Modifier.atFraction(fraction: Float, centered: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = constraints.maxWidth
    layout(width, placeable.height) {
        val x = (width * fraction).roundToInt() - if (centered) placeable.width / 2 else 0
        placeable.placeRelative(x.coerceAtLeast(0), 0)
    }
}

@Composable
private fun NowPointer(glyph: String, fraction: Float) {
    Box(Modifier.fillMaxWidth()) {
        Text(
            glyph,
            color = TeperaPalette.buttonBrandDark,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            modifier = Modifier.atFraction(fraction, centered = true)
        )
    }
}

@Composable
private fun DayStructureLegend(segments: List<DaySegment>) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        segments.forEach { segment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(segment.color))
                    Text(segment.label, fontSize = 12.sp, color = HomeCardTextPrimary)
                }
                // FR-P.6: час завжди поруч із назвою, ніколи голий відсоток самотужки.
                Text(formatClockDuration(segment.minutes), fontSize = 11.sp, color = HomeCardTextPrimary)
            }
        }
    }
}

/** HH:MM локального часу з epoch-мілісекунд. */
private fun formatClock(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

/** Час у форматі `Г:ХХ` (Figma "2:45"), округлений до 15 хв — та сама логіка, що [formatBalanceDuration]. */
private fun formatClockDuration(minutes: Int): String {
    val (hours, remainder) = roundToQuarterHour(minutes)
    return "%d:%02d".format(hours, remainder)
}

/**
 * Округлення до 15 хв (за запитом користувача, замість "245 хв") — напр. "3 год 45 хв",
 * а не "3 год 47 хв".
 */
@Composable
private fun formatBalanceDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}
