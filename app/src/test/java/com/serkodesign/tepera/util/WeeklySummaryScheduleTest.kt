package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WeeklySummaryScheduleTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    // 2026-10-04 — неділя
    @Test
    fun fromMidweek_goesToNextSundayEvening() {
        assertEquals(at(4, 19), WeeklySummarySchedule.nextRunMillis(at(1, 12), zone)) // четвер
    }

    @Test
    fun onSundayBeforeSeven_isSameDay() {
        assertEquals(at(4, 19), WeeklySummarySchedule.nextRunMillis(at(4, 18, 59), zone))
    }

    @Test
    fun onSundayAtOrAfterSeven_isNextWeek() {
        assertEquals(at(11, 19), WeeklySummarySchedule.nextRunMillis(at(4, 19), zone))
        assertEquals(at(11, 19), WeeklySummarySchedule.nextRunMillis(at(4, 22), zone))
    }
}
