package com.serkodesign.tepera.util

import kotlin.math.roundToInt

/**
 * CC-1: стартове значення орієнтира на кроці онбордингу — СЕРЕДНЄ самої людини за останні дні,
 * округлене до цілої години в межах слайдера (1-8 год). Це не норма й не оцінка: лише точка, від
 * якої людина рухає повзунок сама. Без даних (порожня історія) значення немає — тоді екран не
 * стверджує нічого про "середнє".
 */
object TargetSuggestion {
    const val MIN_HOURS = 1
    const val MAX_HOURS = 8

    /** Значення, з якого слайдер стартує, коли історії ще нема (не подається як середнє). */
    const val NEUTRAL_START_HOURS = 3

    fun hoursFor(averageMinutesPerDay: Int): Int =
        (averageMinutesPerDay / 60.0).roundToInt().coerceIn(MIN_HOURS, MAX_HOURS)

    /** Середнє за днями з даними; `null`, якщо жодного дня. */
    fun average(minutesPerDay: List<Int>): Int? =
        if (minutesPerDay.isEmpty()) null else minutesPerDay.sum() / minutesPerDay.size
}
