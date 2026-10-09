package com.serkodesign.tepera.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * P-2 (`CLAUDE-CODE-PLAN-pro.md`): чиста логіка вибору діб для дозапису `day_summaries` — без
 * Room/Context, тестується без пристрою. Перевикористовує [HistoryCoverage] (CC-4) — той самий
 * принцип "лише доби, які системна історія покриває ПОВНІСТЮ".
 */
object DaySummaryBackfill {
    /** Обмежує перший backfill (коли записів ще нема) — реальна глибина `UsageStatsManager`
     * завжди менша (~7-9 діб, `AUDIT-CC-0.md`), це лише верхня межа ітерації по датах. */
    const val MAX_LOOKBACK_DAYS = 400L

    /**
     * Доби-кандидати для запису: завершені (не сьогоднішня), ще не записані ([alreadyRecorded]),
     * і повністю покриті системною історією ([earliestEventMillis], [HistoryCoverage.coveredDays]).
     * `null` у [earliestEventMillis] (нема доступу чи історії) — порожній список.
     */
    fun candidateDays(
        nowMillis: Long,
        alreadyRecorded: Set<LocalDate>,
        earliestEventMillis: Long?,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<LocalDate> {
        if (earliestEventMillis == null) return emptyList()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val yesterday = today.minusDays(1)
        val earliestCandidate = today.minusDays(MAX_LOOKBACK_DAYS)
        if (yesterday.isBefore(earliestCandidate)) return emptyList()
        val unrecorded = generateSequence(earliestCandidate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(yesterday) }
            .filterNot { it in alreadyRecorded }
            .toList()
        return HistoryCoverage.coveredDays(unrecorded, earliestEventMillis, zone)
    }
}
