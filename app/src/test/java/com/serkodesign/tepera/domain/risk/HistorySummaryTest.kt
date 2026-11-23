package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HistorySummaryTest {
    private val d1 = LocalDate.of(2026, 10, 19)
    private val d2 = LocalDate.of(2026, 10, 20)

    @Test
    fun `no days means no summary`() {
        assertNull(HistorySummaries.compute(listOf(SlotUsage(d1, 88, 600)), emptyList()))
    }

    @Test
    fun `no usage in feeds means no summary`() {
        assertNull(HistorySummaries.compute(emptyList(), listOf(d1, d2)))
    }

    @Test
    fun `average is per logical day and peak is the busiest hour`() {
        // d1: 22:00–23:00 → 30 хв (слоти 88,89), d2: 22:00–22:30 → 10 хв; 2 доби → (30+10)/2 = 20 хв на добу.
        val records = listOf(SlotUsage(d1, 88, 900), SlotUsage(d1, 89, 900), SlotUsage(d2, 88, 600))
        val s = HistorySummaries.compute(records, listOf(d1, d2))!!
        assertEquals(2, s.days)
        assertEquals(20, s.avgMinutesPerDay)
        assertEquals(22, s.peakHour)
    }

    @Test
    fun `records outside the given days are ignored`() {
        val records = listOf(SlotUsage(d1, 88, 1200), SlotUsage(LocalDate.of(2026, 10, 1), 88, 3600))
        assertEquals(20, HistorySummaries.compute(records, listOf(d1))!!.avgMinutesPerDay)
    }

    @Test
    fun `peak is dropped when the busiest hour is just noise`() {
        // 10 хв за всі доби — нижче порога MIN_PEAK_MINUTES.
        val s = HistorySummaries.compute(listOf(SlotUsage(d1, 88, 600)), listOf(d1, d2))!!
        assertNull(s.peakHour)
    }

    @Test
    fun `night slots belong to the previous logical day`() {
        // Слот 4 (01:00) на d2 при межі доби о 4:00 — це логічна доба d1.
        val s = HistorySummaries.compute(listOf(SlotUsage(d2, 4, 1200)), listOf(d1))!!
        assertEquals(20, s.avgMinutesPerDay)
        assertEquals(1, s.peakHour)
    }
}
