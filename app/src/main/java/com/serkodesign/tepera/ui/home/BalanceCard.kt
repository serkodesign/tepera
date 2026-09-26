package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.ui.theme.TeperaCard
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaChip
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
                // GAP-2: доступ відкликано пізніше — та сама єдина картка (TeperaCard), що й на Статистиці; головна дія —
                // Secondary (на білій картці біла Primary-кнопка зливалась би з фоном).
                TeperaCard {
                    Text(
                        stringResource(R.string.usage_access_prompt_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TeperaPalette.buttonBrandDark
                    )
                    Text(
                        stringResource(R.string.usage_access_prompt_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
                    )
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
                            type = TeperaButtonType.Secondary
                        )
                    }
                }
            }
            true -> {
                val segments = daySegments(state)
                if (segments.isEmpty()) {
                    // День (за визначенням Tepera, не календарна північ) ще не почався — Online,
                    // категорії й "Решта дня" усі порожні. Без цієї гілки картка рендерила
                    // ЦІЛКОМ ПОРОЖНЄ тіло (жодного тексту) — реальний баг, знайдений користувачем
                    // при відкритті вночі/рано-вранці, до першого суттєвого розблокування.
                    Text(
                        text = stringResource(R.string.home_no_entries_today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HomeCardTextPrimary
                    )
                } else {
                    DayHeader(dayStartMillis = state.dayStartMillis, dayLengthMinutes = state.dayLengthMinutes)
                    DayStructureBar(
                        segments = segments,
                        targetMinutes = state.targetMinutes,
                        daySpanMinutes = state.daySpanMinutes,
                        dayLengthMinutes = state.dayLengthMinutes,
                        dayStartMillis = state.dayStartMillis
                    )
                    DayStructureLegend(segments = segments, targetMinutes = state.targetMinutes)
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
        if (state.restOfDayMinutes > 0) add(DaySegment(restLabel, RestSegmentColor, state.restOfDayMinutes))
    }
}

/**
 * Шапка картки за M3-ієрархією: підпис (label, 14sp) → головне число (headline, 22sp). Підпис і число
 * читаються одним реченням — "З 07:31 минуло / 5 год 45 хв": слово "минуло" і час початку прямо кажуть, що
 * це ВЕСЬ день, а не час у телефоні (велике число поруч із рядком "Online 45 хв" легко прочитати як
 * екранний час). Без часу початку — старий підпис "День триває".
 */
@Composable
private fun DayHeader(dayStartMillis: Long, dayLengthMinutes: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (dayStartMillis > 0L) {
                stringResource(R.string.home_card_day_since_format, formatClock(dayStartMillis))
            } else {
                stringResource(R.string.home_card_day_last_label)
            },
            style = MaterialTheme.typography.labelLarge,
            color = HomeCardTextSecondary
        )
        Text(
            text = formatBalanceDuration(dayLengthMinutes),
            color = TeperaPalette.buttonBrandDark,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 26.sp
        )
    }
}

private val LegendDotSize = 8.dp // кружок-маркер у легенді (за запитом користувача)
private val BarTrackHeight = 20.dp
private val BarTotalHeight = 44.dp // висота з виступами маркерів над/під смугою
private val SegmentGap = 2.dp

// Кольори за запитом користувача: сегмент "решта дня" (те, що ще попереду) — світло-зелений
// "Офлайн-життя" (#C5E2CB), а пройдений сегмент "Без телефону" — глибокий зелений. Так пройдена
// частина читається темнішою й чітко відділена від решти, а в легенді кружок "Без телефону" теж темний.
private val BarTrackColor = TeperaPalette.restOfDayCard
private val RestSegmentColor = Color(0xFF2B5747)

/**
 * Шкала структури доби: пігулка з сегментів (Online, категорії, "Без телефону" і світла "решта дня" = ще не
 * прожитий час), розділених проміжками 2dp (сусідні відтінки не зливаються — читається й без розрізнення кольорів, WCAG 1.4.1).
 * Окремої позначки "зараз" нема: межею є край останнього кольорового сегмента (раніше була чорна
 * ручка, за запитом користувача прибрана). Під шкалою — підписи країв (початок дня → 00:00), щоб
 * було ясно, що саме вона показує.
 *
 * FR-3.10: тиха засічка орієнтиру — тонка напівпрозора лінія БЕЗ підпису, ніколи не змінює колір
 * при перевищенні (розділ 4.3–4.4 SRS: "якщо з'явиться спокуса підсвітити перевищення кольором — це
 * сигнал звірити рішення з розділом 4, не з інтуїцією"). Позиція — частка від повного діапазону
 * шкали (пробудження → 00:00), а не лише від довжини дня, що минула — інакше засічка "стрибала" б
 * праворуч разом з ростом дня.
 */
@Composable
private fun DayStructureBar(
    segments: List<DaySegment>,
    targetMinutes: Int?,
    daySpanMinutes: Int,
    dayLengthMinutes: Int,
    dayStartMillis: Long
) {
    val referenceMinutes = maxOf(daySpanMinutes, targetMinutes ?: 0, 1)
    val markerFraction = targetMinutes?.let { (it.toFloat() / referenceMinutes).coerceIn(0f, 1f) }

    // Без BoxWithConstraints: він робить субкомпозицію на кожному перевимірі, а слайдер Home
    // перевимірює сторінки на кожному кадрі свайпу — на Huawei P9 це давало 63% рваних кадрів.
    // Позиції маркерів рахує легкий layout-модифікатор [atFraction] (без субкомпозиції).
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(BarTotalHeight)) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(BarTrackHeight)
                    .clip(RoundedCornerShape(100.dp)),
                horizontalArrangement = Arrangement.spacedBy(SegmentGap)
            ) {
                // Сегменти можуть перекриватись (Online + запис + офлайн за об'єднанням у сумі більші за довжину
                // дня) — їхні ширини нормалізуються до довжини дня, щоб шкала не виходила за позначку "Now".
                val segmentsTotal = segments.sumOf { it.minutes }
                val scale = if (segmentsTotal > dayLengthMinutes && segmentsTotal > 0) {
                    dayLengthMinutes.toFloat() / segmentsTotal
                } else {
                    1f
                }
                segments.forEach { segment ->
                    Box(
                        modifier = Modifier
                            .weight((segment.minutes * scale).coerceAtLeast(1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(segment.color)
                    )
                }
                val futureMinutes = (daySpanMinutes - dayLengthMinutes).coerceAtLeast(0)
                if (futureMinutes > 0) {
                    // Решта дня — такий самий сегмент, як Online і "Без телефону": займає лише свою частку, а не
                    // лежить суцільною доріжкою під усією шкалою.
                    Box(
                        modifier = Modifier
                            .weight(futureMinutes.toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(BarTrackColor)
                    )
                }
            }
            // CC-1: орієнтир — два тихі трикутники над і під шкалою, без підпису і без зміни кольору.
            if (markerFraction != null) {
                listOf("▼" to Alignment.TopStart, "▲" to Alignment.BottomStart).forEach { (glyph, align) ->
                    Text(
                        text = glyph,
                        color = TeperaPalette.buttonBrandDark,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.align(align).atFraction(markerFraction, centered = true).clearAndSetSemantics { }
                    )
                }
            }
        }
        if (dayStartMillis > 0L) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatClock(dayStartMillis), fontSize = 12.sp, lineHeight = 16.sp, color = HomeCardTextSecondary)
                Text("00:00", fontSize = 12.sp, lineHeight = 16.sp, color = HomeCardTextSecondary)
            }
        }
    }
}

/** Ставить елемент на [fraction] ширини батька (без субкомпозиції, на відміну від BoxWithConstraints). */
private fun Modifier.atFraction(fraction: Float, centered: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = constraints.maxWidth
    layout(width, placeable.height) {
        val x = (width * fraction).roundToInt() - if (centered) placeable.width / 2 else 0
        // Не виходимо за краї смуги: ручка "зараз" у самому кінці дня лишається повністю видимою.
        placeable.placeRelative(x.coerceIn(0, (width - placeable.width).coerceAtLeast(0)), 0)
    }
}

/**
 * Легенда — список "колір · назва · час": кружок, назва, час чіпом праворуч. Завжди ОДНА колонка на всю
 * ширину (за рішенням користувача): у двох колонках довгі назви ("Кулінарія") ламались посеред слова, а чіпси
 * різної ширини розсинхронізовували ряди. Тривалість словами ("3 год 45 хв"), а не "3:45", яке читається як
 * годинник (FR-P.6: час завжди поруч із назвою, ніколи голий відсоток). Картка росте разом зі списком, а
 * пейджер вирівнює за нею сусідні картки.
 */
@Composable
private fun DayStructureLegend(segments: List<DaySegment>, targetMinutes: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Орієнтир (▼ — той самий значок, що на шкалі) дописано в рядок Online, з яким він порівнюється; підпису на самій
        // шкалі нема (FR-3.10). Без Online-сегмента (0 хв) орієнтир лишається окремим рядком.
        val onlineLabel = stringResource(R.string.balance_online_label)
        val onlineIndex = segments.indexOfFirst { it.label == onlineLabel }
        segments.forEachIndexed { index, segment ->
            LegendRow(
                segment,
                compact = false,
                modifier = Modifier.fillMaxWidth(),
                targetMinutes = targetMinutes.takeIf { index == onlineIndex }
            )
        }
        if (targetMinutes != null && onlineIndex < 0) TargetLegendRow(targetMinutes, compact = false)
    }
}

@Composable
private fun TargetLegendRow(targetMinutes: Int, compact: Boolean) {
    val style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 18.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(LegendDotSize), contentAlignment = Alignment.Center) {
            Text("▼", color = TeperaPalette.buttonBrandDark, fontSize = 10.sp, lineHeight = 10.sp)
        }
        Text(
            text = stringResource(R.string.balance_target_label),
            modifier = Modifier.weight(1f),
            style = style,
            color = HomeCardTextPrimary,
            maxLines = 1
        )
        TeperaChip(label = formatBalanceDuration(targetMinutes), compact = true)
    }
}

@Composable
private fun LegendRow(segment: DaySegment, compact: Boolean, modifier: Modifier = Modifier, targetMinutes: Int? = null) {
    val style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    Row(
        modifier = modifier.heightIn(min = if (compact) 18.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(LegendDotSize).clip(CircleShape).background(segment.color))
        Text(
            text = segment.label,
            modifier = Modifier.weight(1f),
            style = style,
            color = HomeCardTextPrimary,
            maxLines = if (compact) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
        if (targetMinutes != null) {
            val targetText = formatBalanceDuration(targetMinutes)
            val targetDescription = stringResource(R.string.balance_target_label) + " " + targetText
            Text(
                text = "▼ $targetText",
                modifier = Modifier.clearAndSetSemantics { contentDescription = targetDescription },
                style = MaterialTheme.typography.bodySmall,
                color = HomeCardTextSecondary,
                maxLines = 1
            )
        }
        TeperaChip(label = formatBalanceDuration(segment.minutes), compact = true)
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
internal fun formatBalanceDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}
