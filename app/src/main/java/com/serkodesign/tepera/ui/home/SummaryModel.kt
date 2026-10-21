package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.data.local.entity.DaySummaryEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Модель підсумків Home FINAL (docs/HANDOFF.md, розділи 5 і 8): тізери "Вчора / Тиждень / Місяць" у hero та шторка.
 * Чиста Kotlin-логіка без Android-залежностей — тестується без пристрою.
 *
 * **Метрика "без телефону" (рішення, яке HANDOFF п. 10.5 просить підтвердити):** для завершеної доби
 * `неспаний час − Online у цей час`, де неспаний час — від точки старту дня до півночі (1440 − `dayStartMinute`),
 * а Online береться з `onlineByHour` лише за години, що лежать після старту дня (нічні перевірки телефону до
 * старту дня не віднімаються). Рахується по годинах, тож однаково годує і суму за день, і стовпчики 24 год.
 * Вікна сну всередині неспаної частини не враховуються (рідкісний випадок — нічна зміна).
 * Тиждень / Місяць — **середнє на день** за завершені доби в межах вікна (сьогодні, ще не завершений, у середнє
 * не входить, але показується останнім стовпчиком).
 */
const val MINUTES_PER_DAY = 24 * 60
const val SUMMARY_WEEK_DAYS = 7
const val SUMMARY_MONTH_DAYS = 30

/** Одна завершена доба в термінах "без телефону". */
data class DayStat(
    val date: LocalDate,
    val startMinute: Int,
    val offlineMinutes: Int,
    /** Online всередині неспаної частини доби. */
    val onlineMinutes: Int,
    /** 24 значення: хвилини "без телефону" по годинах (до старту дня — 0). */
    val offlineByHour: List<Int>,
    /** 24 значення: Online-хвилини по годинах (для широкої плитки "Вчора"). */
    val onlineByHour: List<Int>
)

fun dayStatFromSummary(summary: DaySummaryEntity): DayStat? {
    val start = summary.dayStartMinute ?: return null
    val online = summary.onlineByHour
    if (online.size < 24) return null
    val offlineByHour = ArrayList<Int>(24)
    val onlineAwake = ArrayList<Int>(24)
    for (hour in 0 until 24) {
        val hourStart = hour * 60
        val awakeInHour = (hourStart + 60 - maxOf(hourStart, start)).coerceIn(0, 60)
        val onlineInHour = minOf(online[hour], awakeInHour).coerceAtLeast(0)
        onlineAwake.add(if (awakeInHour > 0) onlineInHour else 0)
        offlineByHour.add((awakeInHour - onlineInHour).coerceAtLeast(0))
    }
    return DayStat(
        date = LocalDate.ofEpochDay(summary.date),
        startMinute = start,
        offlineMinutes = offlineByHour.sum(),
        onlineMinutes = onlineAwake.sum(),
        offlineByHour = offlineByHour,
        onlineByHour = onlineAwake
    )
}

/** Стовпчик/клітинка дня: [offlineMinutes] = null — даних немає. */
data class DayBar(val date: LocalDate, val offlineMinutes: Int?, val isToday: Boolean, val onlineMinutes: Int? = null)

/** Підсумок вікна: середнє "без телефону" на день за завершені доби (null, якщо таких немає) і самі дні. */
data class PeriodSummary(
    val averageOfflineMinutesPerDay: Int?,
    /** Середнє Online на день за ті самі завершені доби (для тізерів). */
    val averageOnlineMinutesPerDay: Int? = null,
    val completedDays: Int,
    val bars: List<DayBar>
)

/**
 * Останні [days] діб, що закінчуються сьогодні. Середнє — лише за завершені доби з даними; сьогодні
 * ([todayOffline], якщо відомо) додається останнім стовпчиком.
 */
fun buildPeriod(today: LocalDate, days: Int, completed: Map<LocalDate, DayStat>, todayOffline: Int?, todayOnline: Int? = null): PeriodSummary {
    val bars = (days - 1 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        if (back == 0) DayBar(date, todayOffline, isToday = true, onlineMinutes = todayOnline)
        else DayBar(date, completed[date]?.offlineMinutes, isToday = false, onlineMinutes = completed[date]?.onlineMinutes)
    }
    val values = bars.filter { !it.isToday }.mapNotNull { it.offlineMinutes }
    val onlineValues = bars.filter { !it.isToday }.mapNotNull { it.onlineMinutes }
    return PeriodSummary(
        averageOfflineMinutesPerDay = if (values.isEmpty()) null else values.sum() / values.size,
        averageOnlineMinutesPerDay = if (onlineValues.isEmpty()) null else onlineValues.sum() / onlineValues.size,
        completedDays = values.size,
        bars = bars
    )
}

/** Календарний тиждень (пн–нд), що містить [today]: для шторки. Майбутні дні лишаються без даних. */
fun buildCalendarWeek(today: LocalDate, completed: Map<LocalDate, DayStat>, todayOffline: Int?, todayOnline: Int? = null): List<DayBar> {
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return (0 until 7).map { i -> calendarBar(monday.plusDays(i.toLong()), today, completed, todayOffline, todayOnline) }
}

/** Календарний місяць, що містить [today], дні 1..N: для шторки. */
fun buildCalendarMonth(today: LocalDate, completed: Map<LocalDate, DayStat>, todayOffline: Int?, todayOnline: Int? = null): List<DayBar> {
    val month = YearMonth.from(today)
    return (1..month.lengthOfMonth()).map { day -> calendarBar(month.atDay(day), today, completed, todayOffline, todayOnline) }
}

private fun calendarBar(date: LocalDate, today: LocalDate, completed: Map<LocalDate, DayStat>, todayOffline: Int?, todayOnline: Int?): DayBar =
    when {
        date == today -> DayBar(date, todayOffline, true, todayOnline)
        date.isAfter(today) -> DayBar(date, null, false)
        else -> DayBar(date, completed[date]?.offlineMinutes, false, completed[date]?.onlineMinutes)
    }

/** Середнє Online на день за завершені доби з даними (сьогодні не входить); null, якщо таких немає. */
fun averageOnline(bars: List<DayBar>): Int? {
    val values = bars.filter { !it.isToday }.mapNotNull { it.onlineMinutes }
    return if (values.isEmpty()) null else values.sum() / values.size
}

/**
 * Вісь Y для стовпчиків Online: верхня межа в годинах і крок позначок. Крок підбирається так, щоб було 2–5 позначок
 * (1, 2, 3 або 6 год), верх — найближче кратне кроку не менше за максимум.
 */
fun onlineAxis(maxMinutes: Int): Pair<Int, Int> {
    val maxHours = maxOf(1, Math.ceil(maxMinutes / 60.0).toInt())
    val step = when {
        maxHours <= 4 -> 1
        maxHours <= 8 -> 2
        maxHours <= 15 -> 3
        else -> 6
    }
    val top = ((maxHours + step - 1) / step) * step
    return top to step
}

/** Середнє на день у годинах для тізера: "4" або "3,7" (одна цифра після коми, лише якщо не ціле). */
fun formatHoursPerDay(minutes: Int, locale: java.util.Locale): String {
    val hours = minutes / 60.0
    val rounded = Math.round(hours * 10) / 10.0
    return if (rounded == Math.rint(rounded)) String.format(locale, "%d", rounded.toInt()) else String.format(locale, "%.1f", rounded)
}

// --- Слот тізерів (HANDOFF, розділ 5) ----------------------------------------------------------------------

enum class SummaryTab { YESTERDAY, WEEK, MONTH }

sealed interface TeaserTile {
    /** Вчора: сума Online + 24 стовпчики по годинах. */
    data class Yesterday(val onlineMinutes: Int, val onlineByHour: List<Int>) : TeaserTile

    /** Вчора без даних: "Вчора — —", без графіка, не натискається. */
    data object YesterdayEmpty : TeaserTile

    /** Тиждень / Місяць: середнє на день + стовпчики (7 або 30), сьогодні виділено останнім. */
    data class Period(val tab: SummaryTab, val averageMinutesPerDay: Int, val bars: List<DayBar>) : TeaserTile
}

sealed interface TeaserSlot {
    /** Пара однакової ширини: ліворуч Вчора, праворуч Тиждень / Місяць. */
    data class Pair(val left: TeaserTile, val right: TeaserTile.Period) : TeaserSlot

    /** Одна широка плитка "Вчора" (запасний варіант): "старт HH:mm" + 24 стовпчики Online. */
    data class WideYesterday(val startMinute: Int, val onlineByHour: List<Int>) : TeaserSlot

    /** Немає даних за обидва періоди (перший день): одна широка Empty-плитка. */
    data object WideEmpty : TeaserSlot
}

/**
 * Правила слота: ліворуч завжди Вчора, праворуч Тиждень (1-го числа — Місяць). Немає тижня/місяця — лише Вчора на
 * всю ширину; немає вчора — "Вчора — —" + права плитка; немає нічого — широка Empty. [forceWide] (шрифт ≥ 130%,
 * ширина < 340dp) — завжди одна широка плитка.
 */
fun buildTeaserSlot(
    yesterday: DayStat?,
    week: PeriodSummary,
    month: PeriodSummary,
    today: LocalDate,
    forceWide: Boolean
): TeaserSlot {
    val rightSource = if (today.dayOfMonth == 1 && month.averageOnlineMinutesPerDay != null) {
        SummaryTab.MONTH to month
    } else {
        SummaryTab.WEEK to week
    }
    val average = rightSource.second.averageOnlineMinutesPerDay
    val right = average?.let { TeaserTile.Period(rightSource.first, it, rightSource.second.bars) }

    if (forceWide || right == null) {
        return if (yesterday != null) TeaserSlot.WideYesterday(yesterday.startMinute, yesterday.onlineByHour) else TeaserSlot.WideEmpty
    }
    val left = if (yesterday != null) TeaserTile.Yesterday(yesterday.onlineMinutes, yesterday.onlineByHour) else TeaserTile.YesterdayEmpty
    return TeaserSlot.Pair(left, right)
}

/** Сума й середнє для шторки: сума "без телефону" та Online за дні вікна з даними. */
data class SheetTotals(val offlineMinutes: Int, val onlineMinutes: Int)

fun sumTotals(bars: List<DayBar>, completed: Map<LocalDate, DayStat>, todayOnline: Int?): SheetTotals {
    var offline = 0
    var online = 0
    bars.forEach { bar ->
        val value = bar.offlineMinutes ?: return@forEach
        offline += value
        online += if (bar.isToday) (todayOnline ?: 0) else (completed[bar.date]?.onlineMinutes ?: 0)
    }
    return SheetTotals(offline, online)
}
