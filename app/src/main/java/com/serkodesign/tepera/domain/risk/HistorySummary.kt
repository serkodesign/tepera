package com.serkodesign.tepera.domain.risk

import java.time.LocalDate

/**
 * «Точка відліку» з системної історії (тиждень спостереження, фаза A плану `first-day-value`): скільки в середньому на
 * добу було в стрічках і в яку годину найбільше. [days] — кількість логічних діб, з яких рахували.
 * [peakHour] — година 0–23 (за годинником), `null`, якщо жодна година не набрала порога.
 */
data class HistorySummary(val days: Int, val avgMinutesPerDay: Int, val peakHour: Int?)

object HistorySummaries {
    /** Година вважається піком, лише якщо за всі доби в ній набралося щонайменше стільки хвилин (не шум). */
    const val MIN_PEAK_MINUTES = 15

    /**
     * [days] — повні логічні доби (покриті разом із наступною); [records] можуть містити й інші дати — вони ігноруються.
     * `null`, якщо діб нема або в стрічках за них не було жодної хвилини (чесно нічого не показуємо).
     */
    fun compute(records: List<SlotUsage>, days: Collection<LocalDate>, cfg: RiskConfig = RiskConfig()): HistorySummary? {
        if (days.isEmpty()) return null
        val daySet = days.toSet()
        var totalSeconds = 0L
        val perHour = IntArray(HeatGrid.HOURS)
        val slotsPerHour = RiskWindowFinder.SLOTS_PER_DAY / HeatGrid.HOURS
        for (r in records) {
            if (RiskWindowFinder.logicalDate(r.date, r.slot, cfg) !in daySet) continue
            totalSeconds += r.seconds
            perHour[r.slot / slotsPerHour] += r.seconds
        }
        val avgMinutes = (totalSeconds / 60.0 / daySet.size).let { Math.round(it).toInt() }
        if (avgMinutes <= 0) return null
        val peak = perHour.indices.maxByOrNull { perHour[it] }
        val peakHour = peak?.takeIf { perHour[it] / 60 >= MIN_PEAK_MINUTES }
        return HistorySummary(daySet.size, avgMinutes, peakHour)
    }
}
