package com.serkodesign.tepera.domain.risk

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * v3.0 MVP, Етап 4: яке нагадування запланувати наступним. Чиста логіка без Android; час і вже показані дні
 * передаються параметрами.
 *
 * Правила (план v3.0): нагадування за [LEAD_MINUTES] хв до початку вікна, не більше одного на календарну добу,
 * лише для ввімкнених вікон із планом. Якщо в один день вікон кілька, першим спрацьовує найближче; після показу
 * решта цього дня блокується через [blockedDates].
 */
object NudgePlanner {
    const val LEAD_MINUTES = 10L

    data class Candidate(
        val windowId: String,
        val startMinute: Int,
        val endMinute: Int,
        val dayType: DayType,
        val enabled: Boolean,
        val hasPlan: Boolean
    )

    data class Nudge(val windowId: String, val fireAt: LocalDateTime, val windowStart: LocalDateTime)

    /**
     * Найближче нагадування після [now] серед придатних вікон або `null`. [blockedDates] — календарні дати (за
     * моментом нагадування), на які нагадування вже показано; на них нічого не планується.
     */
    fun next(
        candidates: List<Candidate>,
        now: LocalDateTime,
        blockedDates: Set<LocalDate>,
        cfg: RiskConfig = RiskConfig()
    ): Nudge? =
        candidates.filter { it.enabled && it.hasPlan }.flatMap { c ->
            (-1..8).mapNotNull { offset ->
                val date = now.toLocalDate().plusDays(offset.toLong())
                // Вікно «лише будні» й початок до 04:00: день за логічною добою (як у NextWindow).
                val logical = if (c.startMinute < cfg.dayCutoffHour * 60) date.minusDays(1) else date
                if (c.dayType == DayType.WEEKDAYS && logical.dayOfWeek.value > 5) return@mapNotNull null
                val start = date.atStartOfDay().plusMinutes(c.startMinute.toLong())
                val fire = start.minusMinutes(LEAD_MINUTES)
                if (!fire.isAfter(now) || fire.toLocalDate() in blockedDates) null else Nudge(c.windowId, fire, start)
            }
        }.minByOrNull { it.fireAt }
}
