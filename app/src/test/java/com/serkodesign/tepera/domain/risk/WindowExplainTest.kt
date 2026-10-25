package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WindowExplainTest {
    private val last = LocalDate.of(2026, 10, 20) // вівторок
    private val week = (6 downTo 0).map { last.minusDays(it.toLong()) }

    @Test
    fun `minutes per app are averaged over hit days and sorted`() {
        val hits = week.take(4)
        // 22:00–23:30 = слоти 88..93. A: 20 хв щодня, B: 10 хв щодня; Z — поза вікном; день поза hits ігнорується.
        val rows = hits.flatMap { d ->
            listOf(
                PackageSlotUsage(d, 88, "a", 1200),
                PackageSlotUsage(d, 89, "b", 600),
                PackageSlotUsage(d, 50, "z", 3600)
            )
        } + PackageSlotUsage(week.last(), 88, "a", 3600)
        val top = WindowExplain.minutesPerApp(rows, hits, 22 * 60, 23 * 60 + 30)
        assertEquals(listOf("a" to 20, "b" to 10), top)
    }

    @Test
    fun `apps with less than half a minute on average are dropped`() {
        val hits = week.take(2)
        val rows = listOf(PackageSlotUsage(hits[0], 88, "tiny", 20))
        assertTrue(WindowExplain.minutesPerApp(rows, hits, 22 * 60, 23 * 60).isEmpty())
    }

    @Test
    fun `no hit days gives no apps`() {
        assertTrue(WindowExplain.minutesPerApp(listOf(PackageSlotUsage(week[0], 88, "a", 600)), emptyList(), 22 * 60, 23 * 60).isEmpty())
    }

    @Test
    fun `missed weekdays are the covered days without a hit`() {
        // week: ср 14, чт 15, пт 16, сб 17, нд 18, пн 19, вт 20. Влучило все, крім середи й суботи.
        val hits = week.filter { it.dayOfWeek != DayOfWeek.WEDNESDAY && it.dayOfWeek != DayOfWeek.SATURDAY }
        assertEquals(
            listOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY),
            WindowExplain.missedWeekdays(week, hits, DayType.ALL)
        )
    }

    @Test
    fun `weekend never counts as missed for a weekday window`() {
        val hits = week.filter { it.dayOfWeek.value <= 5 && it.dayOfWeek != DayOfWeek.FRIDAY }
        assertEquals(listOf(DayOfWeek.FRIDAY), WindowExplain.missedWeekdays(week, hits, DayType.WEEKDAYS))
    }

    @Test
    fun `nothing missed when every day hit`() {
        assertTrue(WindowExplain.missedWeekdays(week, week, DayType.ALL).isEmpty())
    }
}
