package com.serkodesign.tepera.domain.risk

import java.time.LocalDate

/** Частина доби для підпису картки вікна; межі — за початком вікна. */
enum class PartOfDay { MORNING, LUNCH, DAY, EVENING, NIGHT }

/**
 * v3.0 MVP, Етап 3: чиста логіка теплової карти «7 днів × 24 години» (`v3/HeatWeek`) без Android. Рядки — календарні
 * дати, колонки — години від півночі; вікна малюються пунктирними рамками на колонках, які вони покривають.
 */
object HeatGrid {
    const val HOURS = 24

    /** Хвилини в стрічках по годинах для кожної дати з [dates] (рядок = дата, колонка 0 = 00:00–01:00). */
    fun minutesByHour(records: List<SlotUsage>, dates: List<LocalDate>): Array<IntArray> {
        val seconds = dates.associateWith { IntArray(HOURS) }
        val perHour = RiskWindowFinder.SLOTS_PER_DAY / HOURS
        for (r in records) seconds[r.date]?.let { it[r.slot / perHour] += r.seconds }
        return dates.map { d -> IntArray(HOURS) { seconds.getValue(d)[it] / 60 } }.toTypedArray()
    }

    /** Кошик інтенсивності: 0 — порожньо, 1–4 — від слабкого до сильного (хвилини за годину: 1–4, 5–14, 15–29, ≥ 30). */
    fun bucket(minutes: Int): Int = when {
        minutes <= 0 -> 0
        minutes < 5 -> 1
        minutes < 15 -> 2
        minutes < 30 -> 3
        else -> 4
    }

    /**
     * Колонки годин, які покриває вікно: 22:40–00:10 → 22..23 і 0..0; 13:00–13:30 → 13..13. Кінець рівно на годині не
     * займає наступну колонку (11:00 закінчується в колонці 10).
     */
    fun windowColumns(startMinute: Int, endMinute: Int): List<IntRange> {
        val startHour = startMinute / 60
        return if (endMinute > startMinute) {
            listOf(startHour..(endMinute - 1) / 60)
        } else {
            // Перетин півночі; кінець рівно о 00:00 не додає колонки наступної доби.
            listOf(startHour..HOURS - 1) + if (endMinute > 0) listOf(0..(endMinute - 1) / 60) else emptyList()
        }
    }

    fun partOfDay(startMinute: Int): PartOfDay = when (startMinute / 60) {
        in 4..10 -> PartOfDay.MORNING
        in 11..14 -> PartOfDay.LUNCH
        in 15..18 -> PartOfDay.DAY
        in 19..23 -> PartOfDay.EVENING
        else -> PartOfDay.NIGHT
    }
}
