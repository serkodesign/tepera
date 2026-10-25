package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RiskWindowFinderTest {
    private val last = LocalDate.of(2026, 10, 20)
    private val week = (6 downTo 0).map { last.minusDays(it.toLong()) }

    /** Покриті календарні дати: кожна логічна доба тижня + ранок після останньої. */
    private val covered = (week + last.plusDays(1)).toSet()

    private fun slot(h: Int, m: Int) = (h * 60 + m) / 15

    /** 22:00–23:30 (6 слотів по 6 хв) у зазначені дні. */
    private fun evening(days: List<LocalDate>) = days.flatMap { d ->
        (0 until 6).map { SlotUsage(d, slot(22, 0) + it, 360) }
    }

    @Test
    fun `clear evening on six of seven days gives one window`() {
        val result = RiskWindowFinder.find(evening(week.drop(1)), covered, last)
        assertTrue(result.enoughData)
        assertEquals(1, result.candidates.size)
        val w = result.candidates.single()
        assertEquals(22 * 60, w.startMinute)
        assertEquals(23 * 60 + 30, w.endMinute)
        assertEquals(6, w.recurrenceDays)
        assertEquals(36, w.avgMinutes)
    }

    @Test
    fun `usage across midnight belongs to previous logical day`() {
        val records = week.flatMap { d ->
            listOf(SlotUsage(d, slot(23, 30), 600), SlotUsage(d, slot(23, 45), 600)) +
                listOf(SlotUsage(d.plusDays(1), 0, 600), SlotUsage(d.plusDays(1), 1, 600))
        }
        val w = RiskWindowFinder.find(records, covered, last).candidates.single()
        assertEquals(23 * 60 + 30, w.startMinute)
        assertEquals(30, w.endMinute)
        assertTrue(w.crossesMidnight)
        assertEquals(7, w.recurrenceDays)
    }

    @Test
    fun `chaotic schedule gives no windows`() {
        val records = week.mapIndexed { i, d -> SlotUsage(d, slot(8 + i * 2, 0), 900) }
        assertTrue(RiskWindowFinder.find(records, covered, last).candidates.isEmpty())
    }

    @Test
    fun `short usage below average threshold is not a window`() {
        val records = week.map { SlotUsage(it, slot(22, 0), 330) } // 5,5 хв на день
        assertTrue(RiskWindowFinder.find(records, covered, last).candidates.isEmpty())
    }

    @Test
    fun `less than a week of data builds nothing`() {
        val partial = (week.drop(3) + last.plusDays(1)).toSet()
        val result = RiskWindowFinder.find(evening(week), partial, last)
        assertFalse(result.enoughData)
        assertEquals(4, result.observedDays)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun `at most three windows ordered by average minutes`() {
        val hours = listOf(8, 12, 16, 20)
        val records = hours.flatMapIndexed { i, h ->
            week.flatMap { d ->
                listOf(SlotUsage(d, slot(h, 0), 600 + i * 60), SlotUsage(d, slot(h, 15), 600 + i * 60))
            }
        }
        val result = RiskWindowFinder.find(records, covered, last)
        assertEquals(3, result.candidates.size)
        assertEquals(20 * 60, result.candidates.first().startMinute)
    }
}
