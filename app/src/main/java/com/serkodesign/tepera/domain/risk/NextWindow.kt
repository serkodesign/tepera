package com.serkodesign.tepera.domain.risk

import java.time.LocalDateTime

/**
 * v3.0 MVP, Етап 3, L6: яке вікно настане (або вже триває) і коли. Чиста логіка без Android; час передається
 * параметром, тож усе тестується без годинника.
 */
object NextWindow {
    data class Spec(val id: String, val startMinute: Int, val endMinute: Int, val dayType: DayType)

    /** Найближче входження вікна: [startAt]..[endAt]; триває зараз, якщо [startAt] ≤ now. */
    data class Occurrence(val id: String, val startAt: LocalDateTime, val endAt: LocalDateTime) {
        fun isInside(now: LocalDateTime) = !startAt.isAfter(now)
    }

    /**
     * Для кожного вікна — входження, що триває або найближче настане (дивимось від учорашнього дня: вікно через
     * північ могло початись учора). Вікна «лише будні» пропускають суботу й неділю за ЛОГІЧНОЮ добою (початок до 04:00
     * належить попередньому дню). Результат відсортований: те, що триває, потім за часом початку; вікно, якому не
     * знайшлось входження за 8 днів, відсутнє.
     */
    fun occurrences(specs: List<Spec>, now: LocalDateTime, cfg: RiskConfig = RiskConfig()): List<Occurrence> =
        specs.mapNotNull { spec ->
            val length = RiskWindowFinder.lengthMinutes(spec.startMinute, spec.endMinute).toLong()
            (-1..7).asSequence().mapNotNull { offset ->
                val date = now.toLocalDate().plusDays(offset.toLong())
                val logical = if (spec.startMinute < cfg.dayCutoffHour * 60) date.minusDays(1) else date
                if (spec.dayType == DayType.WEEKDAYS && logical.dayOfWeek.value > 5) return@mapNotNull null
                val start = date.atStartOfDay().plusMinutes(spec.startMinute.toLong())
                val end = start.plusMinutes(length)
                if (!end.isAfter(now)) null else Occurrence(spec.id, start, end)
            }.firstOrNull()
        }.sortedBy { it.startAt }
}
