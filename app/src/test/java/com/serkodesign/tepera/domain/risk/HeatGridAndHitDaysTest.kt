package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HeatGridAndHitDaysTest {
    private val last = LocalDate.of(2026, 10, 20) // вівторок
    private val week = (6 downTo 0).map { last.minusDays(it.toLong()) }
    private val covered = (week + last.plusDays(1)).toSet()

    @Test
    fun `minutes by hour sums slots of the same hour`() {
        val d = LocalDate.of(2026, 10, 20)
        val grid = HeatGrid.minutesByHour(
            listOf(SlotUsage(d, 88, 600), SlotUsage(d, 89, 600), SlotUsage(d, 92, 1200), SlotUsage(d.minusDays(5), 0, 60)),
            listOf(d)
        )
        assertEquals(20, grid[0][22]) // слоти 88,89 = 22:00–22:30 → 20 хв
        assertEquals(20, grid[0][23])
        assertEquals(0, grid[0][0])
    }

    @Test
    fun `bucket thresholds`() {
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4), listOf(0, 1, 4, 5, 14, 15, 29, 30).map(HeatGrid::bucket))
    }

    @Test
    fun `window columns inside a day across midnight and ending on the hour`() {
        assertEquals(listOf(13..13), HeatGrid.windowColumns(13 * 60, 13 * 60 + 30))
        assertEquals(listOf(22..23, 0..0), HeatGrid.windowColumns(22 * 60 + 40, 10))
        assertEquals(listOf(10..10), HeatGrid.windowColumns(10 * 60, 11 * 60))
        assertEquals(listOf(22..23), HeatGrid.windowColumns(22 * 60, 0))
    }

    @Test
    fun `part of day follows the window start`() {
        assertEquals(PartOfDay.MORNING, HeatGrid.partOfDay(10 * 60 + 10))
        assertEquals(PartOfDay.LUNCH, HeatGrid.partOfDay(13 * 60))
        assertEquals(PartOfDay.EVENING, HeatGrid.partOfDay(22 * 60 + 40))
        assertEquals(PartOfDay.NIGHT, HeatGrid.partOfDay(1 * 60))
    }

    @Test
    fun `hit days match the days used by the finder`() {
        // 22:00–23:30 на 6 з 7 днів (крім середи 14.10 — перший день тижня).
        val records = week.drop(1).flatMap { d -> (0 until 6).map { SlotUsage(d, 88 + it, 360) } }
        val window = RiskWindowFinder.find(records, covered, last).candidates.single()
        val hits = RiskWindowFinder.hitDays(records, covered, last, window.startMinute, window.endMinute, window.dayType)
        assertEquals(window.hitDays, hits)
        assertEquals(6, hits.size)
    }

    @Test
    fun `weekday window ignores weekend days in hit days`() {
        val records = week.flatMap { d -> listOf(SlotUsage(d, 52, 900), SlotUsage(d, 53, 900)) } // 13:00–13:30 щодня
        val hits = RiskWindowFinder.hitDays(records, covered, last, 13 * 60, 13 * 60 + 30, DayType.WEEKDAYS)
        assertEquals(5, hits.size)
        assertTrue(hits.all { it.dayOfWeek.value <= 5 })
    }
}
