package com.serkodesign.tepera.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
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

/** Локальна північ доби, якій належить довільний [millis] — для попереднього заповнення форми
 * редагування наявного запису (AddEntryScreen), на відміну від [startOfTodayMillis] (завжди сьогодні). */
fun localStartOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Хвилина від півночі для довільного [millis] — той самий сенс, що [currentMinuteOfDay], для не-"зараз" моменту. */
fun minuteOfDay(millis: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
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


/** Година, до якої Home і віджет ще показують попередню добу (за прямим запитом користувача). */
const val DAY_ROLLOVER_HOUR = 1

/**
 * Північ "логічної" доби для Home і віджета: до 01:00 це ще вчорашня доба, тож повертає вчорашню
 * північ, і запит записів "від неї до нескінченності" охоплює і вчорашні записи, і все, що
 * додано/зупинено між 00:00 і 01:00. Календарна [startOfTodayMillis] лишається для Статистики й
 * Щоденника — вони перемикаються рівно опівночі.
 */
fun startOfLogicalDayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    cal.add(Calendar.HOUR_OF_DAY, -DAY_ROLLOVER_HOUR)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Найближчі 01:00 після [nowMillis] — момент, коли Home і віджет перемикаються на нову добу. */
fun nextDayRolloverMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    cal.set(Calendar.HOUR_OF_DAY, DAY_ROLLOVER_HOUR)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    if (cal.timeInMillis <= nowMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
    return cal.timeInMillis
}

/**
 * [startOfLogicalDayMillis] як потік: одразу віддає поточне значення і знову — щойно настає нова
 * логічна доба (о 01:00). Потрібен ViewModel-ям, що живуть довше за одну добу: одноразовий
 * виклик у конструкторі тримав би запити записів прив'язаними до вчорашньої півночі, і Home
 * показував би прогрес попереднього дня. Перевірка щоразу за 30 с, а не одна затримка: `delay` не
 * рахує час глибокого сну пристрою, тож довгий таймер спрацював би пізно.
 */
fun logicalDayStartFlow(
    // Підмінювані годинник і період опитування — лише для тестів; у застосунку діють значення за замовчуванням.
    nowMillis: () -> Long = System::currentTimeMillis,
    pollMillis: Long = 30_000
): Flow<Long> = flow {
    while (true) {
        emit(startOfLogicalDayMillis(nowMillis()))
        delay(pollMillis)
    }
}.distinctUntilChanged()
