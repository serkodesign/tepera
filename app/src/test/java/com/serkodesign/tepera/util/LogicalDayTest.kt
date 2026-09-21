package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class LogicalDayTest {

    private fun at(day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun before_rollover_hour_the_day_is_still_yesterday() {
        assertEquals(at(21, 0, 0), startOfLogicalDayMillis(at(22, 0, 30)))
        assertEquals(at(21, 0, 0), startOfLogicalDayMillis(at(22, 0, 59)))
    }

    @Test
    fun from_rollover_hour_the_new_day_starts() {
        assertEquals(at(22, 0, 0), startOfLogicalDayMillis(at(22, 1, 0)))
        assertEquals(at(22, 0, 0), startOfLogicalDayMillis(at(22, 23, 59)))
    }

    @Test
    fun next_rollover_is_the_upcoming_one_oclock() {
        assertEquals(at(22, 1, 0), nextDayRolloverMillis(at(22, 0, 30)))
        assertEquals(at(23, 1, 0), nextDayRolloverMillis(at(22, 1, 0)))
        assertEquals(at(23, 1, 0), nextDayRolloverMillis(at(22, 15, 0)))
    }
}
