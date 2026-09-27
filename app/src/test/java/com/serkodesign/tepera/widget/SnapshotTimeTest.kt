package com.serkodesign.tepera.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * W-1: `addDaysMillis`/`nextMidnightMillis` мають лишатись коректними на переході літнього й
 * зимового часу (перехід у Європі — остання неділя березня й жовтня; тут узято реальні дати
 * 2026 року для `Europe/Kyiv`: 29.03.2026 02:00→03:00 (навесні, доба 23 год) і 25.10.2026 04:00→03:00
 * (восени, доба 25 год)). Явна timezone у кожному тесті — незалежно від того, де це справді
 * запускається (CI-машина може мати інший часовий пояс за замовчуванням).
 */
class SnapshotTimeTest {

    private val kyiv = TimeZone.getTimeZone("Europe/Kyiv")

    private fun midnight(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(kyiv).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis

    private fun assertIsLocalMidnight(millis: Long) {
        val cal = Calendar.getInstance(kyiv).apply { timeInMillis = millis }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun addDaysMillis_crosses_spring_forward_to_the_correct_wall_clock_midnight() {
        // Doesn't matter that 29.03.2026 has only 23 real hours — result must still be a real midnight.
        val before = midnight(2026, Calendar.MARCH, 28)
        val after = addDaysMillis(before, 1)
        assertIsLocalMidnight(after)
        assertEquals(midnight(2026, Calendar.MARCH, 29), after)
    }

    @Test
    fun addDaysMillis_crosses_fall_back_to_the_correct_wall_clock_midnight() {
        // 25.10.2026 has 25 real hours (clock repeats 03:00-04:00) — still exactly one calendar day later.
        val before = midnight(2026, Calendar.OCTOBER, 24)
        val after = addDaysMillis(before, 1)
        assertIsLocalMidnight(after)
        assertEquals(midnight(2026, Calendar.OCTOBER, 25), after)
    }

    @Test
    fun nextMidnightMillis_from_a_moment_inside_the_day_lands_on_tomorrow() {
        val duringSpringForwardDay = midnight(2026, Calendar.MARCH, 29) + 6 * 3_600_000L // 06:00 local
        assertEquals(midnight(2026, Calendar.MARCH, 30), nextMidnightMillis(duringSpringForwardDay))
    }

    @Test
    fun nextMidnightMillis_from_exact_midnight_still_returns_the_following_day() {
        val exactMidnight = midnight(2026, Calendar.OCTOBER, 25)
        assertEquals(midnight(2026, Calendar.OCTOBER, 26), nextMidnightMillis(exactMidnight))
    }

    @Test
    fun seven_day_sequence_spanning_dst_stays_chronological_and_distinct() {
        val today = midnight(2026, Calendar.MARCH, 31) // 2 days after the spring-forward transition
        val midnights = (6 downTo 0).map { daysAgo -> addDaysMillis(today, -daysAgo) }
        assertEquals(7, midnights.toSet().size) // no two days collapse onto the same instant
        midnights.forEach { assertIsLocalMidnight(it) }
        midnights.zipWithNext().forEach { (earlier, later) -> assert(earlier < later) }
    }
}
