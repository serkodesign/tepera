package com.serkodesign.tepera.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Розбиття активності, що триває через кілька діб, по межах логічної доби (01:00). */
class IntervalSplitterTest {

    private lateinit var previousZone: TimeZone

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun at(month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month, day, hour, minute, 0)
        }.timeInMillis

    private fun sep(day: Int, hour: Int, minute: Int = 0) = at(Calendar.SEPTEMBER, day, hour, minute)

    @Test
    fun interval_inside_one_logical_day_is_not_split() {
        val parts = splitAtDayRollover(sep(19, 9), sep(19, 12))
        assertEquals(1, parts.size)
        assertEquals(180, parts[0].minutes)
    }

    @Test
    fun overnight_activity_is_split_at_the_rollover_hour() {
        // 19 вер 19:00 → 20 вер 12:00: перша частина до 01:00 (6 год), друга від 01:00 (11 год).
        val parts = splitAtDayRollover(sep(19, 19), sep(20, 12))
        assertEquals(2, parts.size)
        assertEquals(sep(19, 19), parts[0].startMillis)
        assertEquals(sep(20, 1), parts[0].endMillis)
        assertEquals(sep(20, 1), parts[1].startMillis)
        assertEquals(sep(20, 12), parts[1].endMillis)
        assertEquals(360, parts[0].minutes)
        assertEquals(660, parts[1].minutes)
    }

    @Test
    fun activity_ending_exactly_at_rollover_stays_in_one_part() {
        val parts = splitAtDayRollover(sep(19, 23), sep(20, 1))
        assertEquals(1, parts.size)
        assertEquals(120, parts[0].minutes)
    }

    @Test
    fun activity_starting_exactly_at_rollover_is_not_split_off() {
        val parts = splitAtDayRollover(sep(20, 1), sep(20, 12))
        assertEquals(1, parts.size)
        assertEquals(sep(20, 1), parts[0].startMillis)
    }

    @Test
    fun short_overnight_activity_around_midnight_is_split_once() {
        // 23:30 → 01:30: до 01:00 (90 хв) і ще 30 хв після.
        val parts = splitAtDayRollover(sep(19, 23, 30), sep(20, 1, 30))
        assertEquals(listOf(90, 30), parts.map { it.minutes })
    }

    @Test
    fun multi_day_activity_gets_one_part_per_logical_day() {
        // 19 вер 19:00 → 22 вер 12:00: 19-го, 20-го, 21-го і 22-го.
        val parts = splitAtDayRollover(sep(19, 19), sep(22, 12))
        assertEquals(4, parts.size)
        assertEquals(listOf(360, 1440, 1440, 660), parts.map { it.minutes })
    }

    @Test
    fun parts_are_contiguous_and_keep_the_total_duration() {
        val start = sep(19, 19, 15)
        val end = sep(23, 7, 40)
        val parts = splitAtDayRollover(start, end)
        assertEquals(start, parts.first().startMillis)
        assertEquals(end, parts.last().endMillis)
        parts.zipWithNext().forEach { (a, b) -> assertEquals(a.endMillis, b.startMillis) }
        assertEquals(((end - start) / 60_000L).toInt(), parts.sumOf { it.minutes })
    }

    @Test
    fun total_duration_survives_the_25_hour_day_when_clocks_go_back() {
        // 25.10.2026 у Києві — 25-годинна доба.
        val start = at(Calendar.OCTOBER, 24, 22)
        val end = at(Calendar.OCTOBER, 25, 12)
        val parts = splitAtDayRollover(start, end)
        assertEquals(2, parts.size)
        assertEquals(((end - start) / 60_000L).toInt(), parts.sumOf { it.minutes })
        assertTrue(parts.all { it.minutes > 0 })
    }

    @Test
    fun invalid_interval_gives_no_parts() {
        assertTrue(splitAtDayRollover(sep(19, 12), sep(19, 12)).isEmpty())
        assertTrue(splitAtDayRollover(sep(19, 12), sep(19, 9)).isEmpty())
    }
}
