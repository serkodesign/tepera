package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.ui.theme.TeperaCard
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.serkodesign.tepera.ui.theme.LocalTeperaColors
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
    // Figma node 347:3166 ("My day"): радіус 24 (не загальні 28dp решти карток пейджера) + біла
    // рамка 1dp — лише в редизайні світлої теми (за запитом користувача темну тему не чіпаємо,
    // борт там лишається вимкненим).
    val isDark = LocalTeperaColors.current.isDark
    HomeCardSurface(
        modifier = modifier,
        containerColor = TeperaPalette.homeCardFillMyDay,
        cornerRadius = 24.dp,
        borderColor = if (isDark) null else Color.White
    ) {
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
                // День (за визначенням Tepera, не календарна північ) ще не почався — Online,
                // категорії й "Решта дня" усі порожні. HomeScreen тепер не додає цю картку до
                // пейджера взагалі в такому стані (показує натомість першу сторінку з реальними
                // даними, напр. Патерн) — гілка нижче лишається як defensive fallback, у
                // нормальному потоці не виконується.
                val segments = daySegments(state)
                if (segments.isNotEmpty()) {
                    DayHeader(dayStartMillis = state.dayStartMillis, dayLengthMinutes = state.dayLengthMinutes)
                    DayStructureBar(
                        segments = segments,
                        targetMinutes = state.targetMinutes,
                        daySpanMinutes = state.daySpanMinutes,
                        dayLengthMinutes = state.dayLengthMinutes,
                        dayStartMillis = state.dayStartMillis
                    )
                    DayStructureLegend(segments = segments)
                }
            }
        }
    }
}

private data class DaySegment(val label: String, val color: Color, val minutes: Int)

/**
 * True, коли є що показати на шкалі "Мій день" (Online, хоча б одна категорія сьогодні, або
 * "Решта дня"). HomeScreen використовує це, щоб узагалі не додавати `MyDayCard` до пейджера,
 * поки день (за точкою старту Tepera) ще не почався — замість порожньої картки одразу
 * показується перша сторінка з реальними даними (Патерн/Цей тиждень).
 */
internal fun BalanceUiState.hasDayData(): Boolean =
    onlineMinutes > 0 || categorySegments.isNotEmpty() || restOfDayMinutes > 0

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
    // Figma node 395:1033: підпис Tag (11sp Regular, text-brand-dark), число — 22sp Medium text-brand (#006944).
    // У темній темі число лишається кремовим (як решта заголовків).
    val valueColor = if (LocalTeperaColors.current.isDark) TeperaPalette.buttonBrandDark else Color(0xFF006944)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = if (dayStartMillis > 0L) {
                stringResource(R.string.home_card_day_since_format, formatClock(dayStartMillis))
            } else {
                stringResource(R.string.home_card_day_last_label)
            },
            fontSize = 11.sp,
            lineHeight = 12.sp,
            color = TeperaPalette.buttonBrandDark
        )
        Text(
            text = formatBalanceDuration(dayLengthMinutes),
            color = valueColor,
            fontFamily = TeperaPalette.headlineFont,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 26.sp
        )
    }
}

private val LegendDotSize = 8.dp // кружок-маркер у легенді (за запитом користувача)
// Figma node 395:1037 (шкала структури дня): білий контейнер, рамка 1dp #DDE2E4, радіус 14, відступ 3dp.
// Висота 40dp → сегменти 32dp (40 − 2 рамки − 6 відступу), орієнтир 37dp (по 2.5dp над і під сегментами).
private val BarFrameHeight = 40.dp
private val BarFrameBorderColor = Color(0xFFDDE2E4)
private val SegmentGap = 1.dp // gap-px у макеті
private val SegmentRadius = 12.dp

// Незайнята частина дня — суцільний бежевий #E2DED1; сегмент "Офлайн" — суцільний зелений #006944, як у чіпі легенди.
private val BarTrackColor = Color(0xFFE2DED1)
// Штрихи на "Решта дня" — світлі діагональні смуги поверх бежевого (як у макеті 395:1033).
private val HatchStripeColor = Color(0x80FFFFFF)
private val RestSegmentColor = Color(0xFF006944)
// Орієнтир (Figma 395:1037): жовтий #F5C401 — заливка 20%, права рамка 1dp без прозорості.
private val TargetFillColor = Color(0x33F5C401)
private val TargetBorderColor = Color(0xFFF5C401)

/**
 * Шкала структури доби: пігулка з сегментів (Online, категорії, "Без телефону" і світла "решта дня" = ще не
 * прожитий час), розділених проміжками 1dp за макетом (сусідні відтінки не зливаються — читається й без розрізнення кольорів, WCAG 1.4.1).
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
    // Ширина орієнтиру — частка від ширини смуги (fillMaxWidth(fraction)), без субкомпозиції.
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(BarFrameHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, BarFrameBorderColor, RoundedCornerShape(14.dp))
        ) {
            // Внутрішня зона всередині рамки (1dp): орієнтир від її початку до позначки, за сегментами.
            Box(Modifier.fillMaxSize().padding(1.dp)) {
                if (markerFraction != null) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(markerFraction)
                            .height(37.dp)
                            .background(TargetFillColor)
                            .drawBehind {
                                val border = 2.dp.toPx()
                                drawLine(
                                    TargetBorderColor,
                                    Offset(size.width - border / 2, 0f),
                                    Offset(size.width - border / 2, size.height),
                                    strokeWidth = border
                                )
                            }
                    )
                }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
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
                            .clip(RoundedCornerShape(SegmentRadius))
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
                            .clip(RoundedCornerShape(SegmentRadius))
                            .hatched(BarTrackColor)
                    )
                }
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



/**
 * Легенда — чіпси "колір · назва · час" (Figma "App concept" node 347:3037, "My day": замінює
 * попередній вертикальний список рядків за прямим запитом користувача — "статистика по
 * активностям на картці подається у вигляді чіпсів"). `FlowRow` переносить чіпси на новий рядок,
 * коли вони не влазять в ширину картки; порядок — той самий, що й раніше (Online → категорії →
 * "Решта дня"). Тривалість словами ("3 год 45 хв"), не "3:45" (FR-P.6: час завжди поруч із
 * назвою, ніколи голий відсоток).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayStructureLegend(segments: List<DaySegment>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        segments.forEach { segment -> LegendChip(segment) }
    }
}

@Composable
private fun LegendChipSurface(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.chipSurface)
            .border(1.dp, TeperaPalette.inputSurface, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun LegendChip(segment: DaySegment) {
    LegendChipSurface {
        Box(Modifier.size(LegendDotSize).clip(RoundedCornerShape(2.dp)).background(segment.color))
        Text(segment.label, fontSize = 12.sp, color = HomeCardTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatBalanceDuration(segment.minutes), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = HomeCardTextPrimary, maxLines = 1)
    }
}

/** Діагональне штрихування поверх [base] (Figma: "Решта дня" — незайнята частина дня), без субкомпозиції. */
private fun Modifier.hatched(base: Color): Modifier = drawBehind {
    drawRect(base)
    val step = 6.dp.toPx()
    val stroke = 2.dp.toPx()
    // Штрихи починаються ЗА межами області (від -висоти), тож обрізаємо по прямокутнику сегмента.
    clipRect(0f, 0f, size.width, size.height) {
        var x = -size.height
        while (x < size.width) {
            drawLine(HatchStripeColor, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stroke)
            x += step
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
internal fun formatBalanceDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}
