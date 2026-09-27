package com.serkodesign.tepera.widget

import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.util.localStartOfDay

private const val DEFAULT_DAYS = 7

/**
 * W-1 / D-33: тижневий патерн рахується ЛИШЕ з живої історії `UsageStatsManager` — власник
 * скасував фонові щоденні знімки (CC-4, D-25), тож глибина обмежена тим, що реально повертає
 * система (~7–9 днів на S23, `docs/AUDIT-CC-0.md` розділ 4; менше на слабших/старіших пристроях
 * чи одразу після встановлення —W-5 сам вирішує, чи цього достатньо для показу).
 */
class WeekSnapshotRepository(private val balanceRepository: BalanceRepository) {

    suspend fun buildForLastDays(nowMillis: Long = System.currentTimeMillis(), days: Int = DEFAULT_DAYS): WeekSnapshot {
        val today = localStartOfDay(nowMillis)
        // Хронологічний порядок: найдавніший день перший, сьогодні останнім.
        val midnights = (days - 1 downTo 0).map { daysAgo -> addDaysMillis(today, -daysAgo) }
        val hasAccess = balanceRepository.hasUsageAccess()
        val perDay = midnights.map { midnight ->
            if (hasAccess) balanceRepository.getOnlineMinutesPerSlot(midnight, 60, 24) else IntArray(24)
        }
        return buildWeekSnapshot(midnights, perDay)
    }
}
