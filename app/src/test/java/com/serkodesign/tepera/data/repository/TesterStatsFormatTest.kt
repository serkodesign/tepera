package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.domain.risk.DayType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TesterStatsFormatTest {
    private val range = LocalDate.of(2026, 10, 18) to LocalDate.of(2026, 10, 24)

    private fun stats(windows: List<TesterWindowStat>) = TesterStats(
        firstDataDate = LocalDate.of(2026, 10, 16), coveredDays = 10, feedAppCount = 3,
        baselineRange = range, currentRange = LocalDate.of(2026, 10, 25) to LocalDate.of(2026, 10, 31),
        windows = windows, nudgesScheduled = 4, nudgesShown = 3, nudgesOk = 2, nudgesNotToday = 1
    )

    private fun window(baseline: Double?, current: Double?, dayType: DayType = DayType.ALL) = TesterWindowStat(
        startMinute = 610, endMinute = 680, dayType = dayType, recurrenceDays = 5, observedDays = 7, avgMinutes = 30,
        isUserDisabled = false, hasPlan = true, shiftSuggested = false, baselineMinutes = baseline, currentMinutes = current
    )

    @Test
    fun `report contains window times recurrence minutes and nudge counts`() {
        val text = TesterStatsRepository.format(stats(listOf(window(31.4, 18.0))))
        assertTrue(text.contains("10:10–11:20"))
        assertTrue(text.contains("повторюваність 5/7"))
        assertTrue(text.contains("спостереження 31 хв → поточний тиждень 18 хв"))
        assertTrue(text.contains("заплановано 4, показано 3, «Добре» 2, «Не сьогодні» 1"))
        assertTrue(text.contains("є план"))
    }

    @Test
    fun `missing numbers are shown as dash and weekday windows are tagged`() {
        val text = TesterStatsRepository.format(stats(listOf(window(null, null, DayType.WEEKDAYS))))
        assertTrue(text.contains("спостереження — → поточний тиждень —"))
        assertTrue(text.contains("будні"))
    }

    @Test
    fun `no windows says so and honest note about baseline is always present`() {
        val text = TesterStatsRepository.format(stats(emptyList()))
        assertTrue(text.contains("Вікон нема"))
        assertTrue(text.contains("орієнтовне"))
        assertFalse(text.contains("Вікно "))
    }
}
