package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import kotlinx.coroutines.launch

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

/** Перехід логічної доби о 01:00: потік Home, а також літній/зимовий час (Europe/Kyiv). */
class LogicalDayRolloverTest {

    private val originalZone: java.util.TimeZone = java.util.TimeZone.getDefault()

    @org.junit.Before
    fun useKyivZone() = java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Kyiv"))

    @org.junit.After
    fun restoreZone() = java.util.TimeZone.setDefault(originalZone)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, second)
        }.timeInMillis

    @Test
    fun home_flow_switches_to_the_new_day_when_the_clock_crosses_one_oclock() = kotlinx.coroutines.runBlocking {
        val clock = java.util.concurrent.atomic.AtomicLong(at(2026, Calendar.SEPTEMBER, 22, 0, 59, 40))
        val emitted = java.util.Collections.synchronizedList(mutableListOf<Long>())
        val job = kotlinx.coroutines.GlobalScope.launch {
            logicalDayStartFlow(nowMillis = { clock.get() }, pollMillis = 1).collect { emitted.add(it) }
        }
        kotlinx.coroutines.delay(60)
        // Ще 00:59:40 — доба лишається вчорашньою (21-е), нового значення нема.
        assertEquals(listOf(at(2026, Calendar.SEPTEMBER, 21, 0, 0)), emitted.toList())

        clock.set(at(2026, Calendar.SEPTEMBER, 22, 1, 0, 5))
        kotlinx.coroutines.delay(120)
        job.cancel()
        // Після 01:00 потік віддав рівно одне нове значення — північ 22-го (distinctUntilChanged, без повторів).
        assertEquals(
            listOf(at(2026, Calendar.SEPTEMBER, 21, 0, 0), at(2026, Calendar.SEPTEMBER, 22, 0, 0)),
            emitted.toList()
        )
    }

    @Test
    fun midnight_itself_does_not_switch_the_day() {
        val before = startOfLogicalDayMillis(at(2026, Calendar.SEPTEMBER, 21, 23, 59, 59))
        val after = startOfLogicalDayMillis(at(2026, Calendar.SEPTEMBER, 22, 0, 0, 1))
        assertEquals(at(2026, Calendar.SEPTEMBER, 21, 0, 0), before)
        assertEquals(before, after)
    }

    @Test
    fun day_of_the_autumn_clock_change_is_25_hours_and_still_rolls_at_one_oclock() {
        // 25 жовтня 2026: о 04:00 годинник іде назад на 03:00 — у цій добі 25 годин.
        val midnight = at(2026, Calendar.OCTOBER, 25, 0, 0)
        assertEquals(at(2026, Calendar.OCTOBER, 24, 0, 0), startOfLogicalDayMillis(at(2026, Calendar.OCTOBER, 25, 0, 30)))
        assertEquals(midnight, startOfLogicalDayMillis(at(2026, Calendar.OCTOBER, 25, 1, 0)))
        // Після 01:00 наступного разу перехід — 01:00 26-го (через 25-годинну добу), не "через 24 години".
        assertEquals(
            at(2026, Calendar.OCTOBER, 26, 1, 0),
            nextDayRolloverMillis(at(2026, Calendar.OCTOBER, 25, 2, 0))
        )
    }

    @Test
    fun day_of_the_spring_clock_change_still_rolls_at_one_oclock() {
        // 29 березня 2026: о 03:00 годинник іде вперед на 04:00 — 01:00 існує, добу не зсуває.
        assertEquals(at(2026, Calendar.MARCH, 28, 0, 0), startOfLogicalDayMillis(at(2026, Calendar.MARCH, 29, 0, 30)))
        assertEquals(at(2026, Calendar.MARCH, 29, 0, 0), startOfLogicalDayMillis(at(2026, Calendar.MARCH, 29, 1, 0)))
        assertEquals(at(2026, Calendar.MARCH, 30, 1, 0), nextDayRolloverMillis(at(2026, Calendar.MARCH, 29, 5, 0)))
    }
}
