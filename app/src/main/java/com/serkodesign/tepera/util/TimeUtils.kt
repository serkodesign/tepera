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
