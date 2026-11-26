package com.serkodesign.tepera.analytics

import com.serkodesign.tepera.data.local.entity.DailyEventCounterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsShareReportTest {
    private val today = LocalDate.of(2026, 11, 26)
    private fun row(daysAgo: Long, name: String, props: String, count: Int) =
        DailyEventCounterEntity(today.minusDays(daysAgo), name, props, count)

    @Test
    fun sums_across_days_and_sorts() {
        val text = StatsShareReport.build(
            listOf(
                row(0, "tab_viewed", "tab=data", 2),
                row(3, "tab_viewed", "tab=data", 3),
                row(1, "activity_added", "", 4),
                row(1, "tab_viewed", "tab=today", 1)
            ),
            today, "1.3.8", "15", 35
        )
        assertEquals(
            """
            Tepera anonymous usage statistics
            Period: 2026-10-28 – 2026-11-26 (30 days)
            App version: 1.3.8
            Android: 15 (API 35)

            activity_added: 4
            tab_viewed [tab=data]: 5
            tab_viewed [tab=today]: 1
            """.trimIndent(),
            text
        )
    }

    @Test
    fun ignores_rows_outside_period() {
        val rows = listOf(row(30, "old", "", 9), row(0, "new", "", 1))
        val text = StatsShareReport.build(rows, today, "1", "15", 35)
        assertFalse(text.contains("old"))
        assertTrue(text.contains("new: 1"))
    }

    @Test
    fun empty_report_and_hasData() {
        val text = StatsShareReport.build(emptyList(), today, "1", "15", 35)
        assertTrue(text.endsWith("No events yet."))
        assertFalse(StatsShareReport.hasData(listOf(row(40, "x", "", 1)), today))
        assertTrue(StatsShareReport.hasData(listOf(row(29, "x", "", 1)), today))
    }

    @Test
    fun report_has_no_identifiers() {
        val text = StatsShareReport.build(listOf(row(0, "tab_viewed", "tab=data", 1)), today, "1.3.8", "15", 35)
        assertFalse(Regex("\\bid\\b").containsMatchIn(text.lowercase()))
        assertFalse(text.contains("@"))
    }
}
