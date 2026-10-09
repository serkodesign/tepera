package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** P-2 (`CLAUDE-CODE-PLAN-pro.md`): чиста логіка вибору діб для `day_summaries`. */
class DaySummaryBackfillTest {

    private val zone = ZoneId.of("Europe/Kyiv")

    private fun at(d: LocalDate, h: Int = 12): Long =
        LocalDateTime.of(d.year, d.monthValue, d.dayOfMonth, h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun excludesToday_onlyCompletedDaysAreCandidates() {
        val today = LocalDate.of(2026, 10, 10)
        val earliest = at(today.minusDays(5))
        val candidates = DaySummaryBackfill.candidateDays(at(today), emptySet(), earliest, zone)
        assertTrue(today !in candidates)
        assertEquals(today.minusDays(1), candidates.last())
    }

    @Test
    fun excludesAlreadyRecordedDays_noDuplicatesOnReopen() {
        val today = LocalDate.of(2026, 10, 10)
        val earliest = at(today.minusDays(5), h = 0) // рівно північ — доба -5 покрита повністю
        val recorded = setOf(today.minusDays(1), today.minusDays(2))
        val candidates = DaySummaryBackfill.candidateDays(at(today), recorded, earliest, zone)
        assertEquals(listOf(today.minusDays(5), today.minusDays(4), today.minusDays(3)), candidates)
    }

    @Test
    fun excludesDaysOlderThanHistory_partiallyCoveredDayIsNotWritten() {
        val today = LocalDate.of(2026, 10, 10)
        // Історія починається в середині 5-го дня тому — той день покритий НЕПОВНІСТЮ, не входить.
        val earliest = at(today.minusDays(5), h = 14)
        val candidates = DaySummaryBackfill.candidateDays(at(today), emptySet(), earliest, zone)
        assertTrue(today.minusDays(5) !in candidates)
        assertEquals(today.minusDays(4), candidates.first())
    }

    @Test
    fun noAccessOrNoHistory_returnsEmpty_doesNotCrash() {
        val today = LocalDate.of(2026, 10, 10)
        assertEquals(emptyList<LocalDate>(), DaySummaryBackfill.candidateDays(at(today), emptySet(), null, zone))
    }

    @Test
    fun nothingNewToWrite_whenEverythingAlreadyRecorded() {
        val today = LocalDate.of(2026, 10, 10)
        val earliest = at(today.minusDays(2))
        val recorded = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(emptyList<LocalDate>(), DaySummaryBackfill.candidateDays(at(today), recorded, earliest, zone))
    }

    @Test
    fun dstFallBack_25HourDay_doesNotSkipOrDuplicateADay() {
        // 25.10.2026 — перехід на зимовий час у Europe/Kyiv (доба 25 год).
        val today = LocalDate.of(2026, 10, 27)
        val earliest = at(LocalDate.of(2026, 10, 23), h = 0)
        val candidates = DaySummaryBackfill.candidateDays(at(today), emptySet(), earliest, zone)
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 23),
                LocalDate.of(2026, 10, 24),
                LocalDate.of(2026, 10, 25), // та сама 25-годинна доба — рівно один запис, не два
                LocalDate.of(2026, 10, 26)
            ),
            candidates
        )
    }

    @Test
    fun dstSpringForward_23HourDay_doesNotSkipOrDuplicateADay() {
        // 29.03.2026 — перехід на літній час у Europe/Kyiv (доба 23 год).
        val today = LocalDate.of(2026, 4, 1)
        val earliest = at(LocalDate.of(2026, 3, 27), h = 0)
        val candidates = DaySummaryBackfill.candidateDays(at(today), emptySet(), earliest, zone)
        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 3, 29), // та сама 23-годинна доба — рівно один запис
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 3, 31)
            ),
            candidates
        )
    }
}
