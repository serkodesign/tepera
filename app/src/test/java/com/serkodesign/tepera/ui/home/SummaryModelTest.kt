package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.data.local.entity.DaySummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SummaryModelTest {
    private val today = LocalDate.of(2026, 10, 15) // четвер

    private fun summary(date: LocalDate, startMinute: Int?, onlineByHour: IntArray = IntArray(24)) = DaySummaryEntity(
        date = date.toEpochDay(), onlineMinutes = onlineByHour.sum(), unlocks = null, dayStartMinute = startMinute,
        lastUseMinute = null, onlineByHour = onlineByHour, exclusionHash = 0, sleepWindowHash = 0, createdAt = 0L
    )

    private fun stat(date: LocalDate, start: Int = 6 * 60, online: Int = 0): DayStat {
        // Online розкладається по годинах від 10:00 (година вміщує максимум 60 хв).
        val byHour = IntArray(24)
        var left = online
        var hour = 10
        while (left > 0) {
            byHour[hour] = minOf(60, left)
            left -= byHour[hour]
            hour++
        }
        return dayStatFromSummary(summary(date, start, byHour))!!
    }

    @Test
    fun noDayStartMeansNoStat() {
        assertNull(dayStatFromSummary(summary(today.minusDays(1), null)))
    }

    @Test
    fun offlineIsAwakeTimeMinusOnlineAfterDayStart() {
        // старт 06:00 → неспано 18 год = 1080 хв; Online 120 хв о 10:00 → без телефону 960
        val s = stat(today.minusDays(1), start = 6 * 60, online = 60).let {
            dayStatFromSummary(summary(today.minusDays(1), 6 * 60, IntArray(24).also { a -> a[10] = 60; a[11] = 60 }))!!
        }
        assertEquals(960, s.offlineMinutes)
        assertEquals(120, s.onlineMinutes)
        assertEquals(24, s.offlineByHour.size)
        assertEquals(0, s.offlineByHour[3]) // до старту дня
        assertEquals(0, s.offlineByHour[10]) // година повністю в Online
        assertEquals(60, s.offlineByHour[12])
    }

    @Test
    fun nightOnlineBeforeDayStartIsNotSubtracted() {
        val byHour = IntArray(24).also { it[1] = 30 } // 01:00 — до старту дня
        val s = dayStatFromSummary(summary(today.minusDays(1), 7 * 60, byHour))!!
        assertEquals(17 * 60, s.offlineMinutes)
        assertEquals(0, s.onlineMinutes)
    }

    @Test
    fun partialStartHourCountsOnlyAwakePart() {
        val s = dayStatFromSummary(summary(today.minusDays(1), 6 * 60 + 30))!!
        assertEquals(30, s.offlineByHour[6])
        assertEquals(60, s.offlineByHour[7])
    }

    @Test
    fun periodAverageUsesCompletedDaysOnlyAndTodayIsLastBar() {
        val completed = mapOf(
            today.minusDays(1) to stat(today.minusDays(1)),
            today.minusDays(2) to stat(today.minusDays(2), online = 120)
        )
        val period = buildPeriod(today, 7, completed, todayOffline = 50)
        assertEquals(7, period.bars.size)
        assertTrue(period.bars.last().isToday)
        assertEquals(50, period.bars.last().offlineMinutes)
        assertEquals(2, period.completedDays)
        // 1080 і 960 → 1020
        assertEquals(1020, period.averageOfflineMinutesPerDay)
        assertNull(period.bars.first().offlineMinutes)
    }

    @Test
    fun noCompletedDaysGivesNullAverage() {
        assertNull(buildPeriod(today, 7, emptyMap(), 100).averageOfflineMinutesPerDay)
    }

    @Test
    fun calendarWeekStartsOnMondayAndFutureIsEmpty() {
        val week = buildCalendarWeek(today, mapOf(today.minusDays(1) to stat(today.minusDays(1))), 200)
        assertEquals(java.time.DayOfWeek.MONDAY, week.first().date.dayOfWeek)
        assertEquals(today, week[3].date)
        assertEquals(200, week[3].offlineMinutes)
        assertNull(week[4].offlineMinutes)
        assertNull(week[0].offlineMinutes) // пн без даних
        assertEquals(1080, week[2].offlineMinutes)
    }

    @Test
    fun calendarMonthHasAllDays() {
        assertEquals(31, buildCalendarMonth(today, emptyMap(), null).size)
    }

    private fun slot(yesterday: DayStat?, week: PeriodSummary, month: PeriodSummary = buildPeriod(today, 30, emptyMap(), null), day: LocalDate = today, wide: Boolean = false) =
        buildTeaserSlot(yesterday, week, month, day, wide)

    private val weekWithData get() = buildPeriod(today, 7, mapOf(today.minusDays(1) to stat(today.minusDays(1))), 10)
    private val empty get() = buildPeriod(today, 7, emptyMap(), null)

    @Test
    fun pairWhenBothAvailable() {
        val result = slot(stat(today.minusDays(1)), weekWithData)
        assertTrue(result is TeaserSlot.Pair)
        assertEquals(SummaryTab.WEEK, ((result as TeaserSlot.Pair).right).tab)
    }

    @Test
    fun noPeriodDataFallsBackToWideYesterday() {
        assertTrue(slot(stat(today.minusDays(1)), empty) is TeaserSlot.WideYesterday)
    }

    @Test
    fun noYesterdayKeepsPairWithEmptyLeft() {
        val result = slot(null, weekWithData) as TeaserSlot.Pair
        assertEquals(TeaserTile.YesterdayEmpty, result.left)
    }

    @Test
    fun nothingAtAllIsWideEmpty() {
        assertEquals(TeaserSlot.WideEmpty, slot(null, empty))
    }

    @Test
    fun forceWideAlwaysSingleTile() {
        assertTrue(slot(stat(today.minusDays(1)), weekWithData, wide = true) is TeaserSlot.WideYesterday)
        assertEquals(TeaserSlot.WideEmpty, slot(null, weekWithData, wide = true))
    }

    @Test
    fun firstOfMonthShowsMonthOnTheRight() {
        val first = LocalDate.of(2026, 11, 1)
        val month = buildPeriod(first, 30, mapOf(first.minusDays(1) to stat(first.minusDays(1))), null)
        val result = buildTeaserSlot(stat(first.minusDays(1)), buildPeriod(first, 7, emptyMap(), null), month, first, false) as TeaserSlot.Pair
        assertEquals(SummaryTab.MONTH, result.right.tab)
    }

    @Test
    fun hoursPerDayFormat() {
        val uk = java.util.Locale("uk")
        assertEquals("4", formatHoursPerDay(240, uk))
        assertEquals("3,7", formatHoursPerDay(222, uk))
        assertEquals("0", formatHoursPerDay(0, uk))
        assertEquals("3.7", formatHoursPerDay(222, java.util.Locale.US))
    }

    @Test
    fun totalsSumDaysWithDataAndUseTodayOnline() {
        val completed = mapOf(today.minusDays(1) to stat(today.minusDays(1), online = 60))
        val bars = buildCalendarWeek(today, completed, 100)
        val totals = sumTotals(bars, completed, todayOnline = 30)
        assertEquals(1020 + 100, totals.offlineMinutes)
        assertEquals(60 + 30, totals.onlineMinutes)
    }
}
