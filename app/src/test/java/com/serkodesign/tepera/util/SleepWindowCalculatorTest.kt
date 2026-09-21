package com.serkodesign.tepera.util

import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** FR-3.2 (SRS v4): арифметика вікон сну покрита юніт-тестами — без пристрою й без Room. */
class SleepWindowCalculatorTest {

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, day, hour, minute, 0)
        }.timeInMillis

    private fun window(startHour: Int, endHour: Int, slot: Int = 1, enabled: Boolean = true) =
        SleepWindowEntity(slot, startHour * 60, endHour * 60, enabled)

    @Test
    fun default_window_counts_minutes_inside_a_single_day() {
        val windows = listOf(window(0, 6))
        assertEquals(360, SleepWindowCalculator.minutesInWindows(windows, at(22, 0), at(22, 12)))
        assertEquals(60, SleepWindowCalculator.minutesInWindows(windows, at(22, 5), at(22, 12)))
        assertEquals(0, SleepWindowCalculator.minutesInWindows(windows, at(22, 7), at(22, 12)))
    }

    @Test
    fun window_that_crosses_midnight_is_counted_on_both_sides() {
        val windows = listOf(window(23, 7))
        // 22:00 → 08:00 наступного дня: 23:00–07:00 = 8 год.
        assertEquals(480, SleepWindowCalculator.minutesInWindows(windows, at(22, 22), at(23, 8)))
        assertTrue(SleepWindowCalculator.isInsideWindow(windows, at(22, 23, 30)))
        assertTrue(SleepWindowCalculator.isInsideWindow(windows, at(23, 3)))
        assertFalse(SleepWindowCalculator.isInsideWindow(windows, at(23, 7)))
    }

    @Test
    fun daytime_window_for_night_shift_is_supported() {
        val windows = listOf(window(9, 16))
        assertEquals(420, SleepWindowCalculator.minutesInWindows(windows, at(22, 0), at(23, 0)))
        assertFalse(SleepWindowCalculator.isInsideWindow(windows, at(22, 8, 59)))
        assertTrue(SleepWindowCalculator.isInsideWindow(windows, at(22, 9)))
    }

    @Test
    fun disabled_window_and_empty_window_are_ignored() {
        assertEquals(0, SleepWindowCalculator.minutesInWindows(listOf(window(0, 6, enabled = false)), at(22, 0), at(22, 12)))
        assertEquals(0, SleepWindowCalculator.minutesInWindows(listOf(window(6, 6)), at(22, 0), at(23, 0)))
        assertEquals(0, SleepWindowCalculator.minutesInWindows(emptyList(), at(22, 0), at(23, 0)))
    }

    @Test
    fun overlapping_windows_are_not_double_counted() {
        // Слот 1 00:00–06:00 і слот 2 03:00–08:00 перекриваються на 3 год → разом 8 год, не 11.
        val windows = listOf(window(0, 6), window(3, 8, slot = 2))
        assertEquals(480, SleepWindowCalculator.minutesInWindows(windows, at(22, 0), at(22, 12)))
    }

    @Test
    fun empty_or_reversed_range_yields_zero_minutes() {
        val windows = listOf(window(0, 6))
        assertEquals(0, SleepWindowCalculator.minutesInWindows(windows, at(22, 3), at(22, 3)))
        assertEquals(0, SleepWindowCalculator.minutesInWindows(windows, at(22, 4), at(22, 3)))
    }

    @Test
    fun overlap_and_full_coverage_checks() {
        val windows = listOf(window(0, 6))
        assertTrue(SleepWindowCalculator.overlapsWindow(windows, at(22, 5, 30), at(22, 6, 30)))
        assertFalse(SleepWindowCalculator.overlapsWindow(windows, at(22, 6), at(22, 7)))
        assertTrue(SleepWindowCalculator.isFullyCovered(windows, at(22, 1), at(22, 5)))
        assertFalse(SleepWindowCalculator.isFullyCovered(windows, at(22, 5), at(22, 7)))
    }
}
