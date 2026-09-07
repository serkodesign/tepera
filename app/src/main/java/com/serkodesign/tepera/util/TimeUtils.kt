package com.serkodesign.tepera.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Локальна північ поточної доби. Узгоджується з відомим timezone-обмеженням MVP (SRS розділ 11) —
 * так само, як BalanceRepository.startOfTodayMillis(), не обробляє зміну часового поясу.
 */
fun startOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun currentMinuteOfDay(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

/**
 * Спільний поділ доби на 4 періоди — використовується і для сортування кнопок віджета (FR-4.5,
 * WidgetLogic.sortCategoriesForWidget), і для привітання на Home ("Доброго ранку" тощо, Figma-
 * фрейм Everyday_Designs). Робочі межі, не з SRS буквально.
 */
enum class DayPeriod { MORNING, DAY, EVENING, NIGHT }

fun currentDayPeriod(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): DayPeriod = when (hourOfDay) {
    in 5..10 -> DayPeriod.MORNING
    in 11..17 -> DayPeriod.DAY
    in 18..22 -> DayPeriod.EVENING
    else -> DayPeriod.NIGHT
}

/**
 * Material3 DatePicker повертає обрану дату як UTC-північ (selectedDateMillis), не локальну
 * (задокументована особливість API). Конвертує в локальну північ того ж календарного дня,
 * інакше в часових поясах на схід від UTC (напр. Київ) дата могла б "з'їхати" на день раніше.
 */
fun utcMidnightToLocalStartOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val local = Calendar.getInstance().apply {
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        set(Calendar.MILLISECOND, 0)
    }
    return local.timeInMillis
}
