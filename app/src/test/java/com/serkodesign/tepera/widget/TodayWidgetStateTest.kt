package com.serkodesign.tepera.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TodayWidgetStateTest {

    private fun at(hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.SEPTEMBER, 14, hour, 0, 0)
    }.timeInMillis

    @Test
    fun no_access_wins_over_everything_else() {
        val state = resolveTodayWidgetState(
            hasUsageAccess = false, isInsideSleepWindow = true, gapsEmpty = true, nowMillis = at(3)
        )
        assertEquals(TodayWidgetState.NO_ACCESS, state)
    }

    @Test
    fun night_window_wins_over_morning_quiet() {
        val state = resolveTodayWidgetState(
            hasUsageAccess = true, isInsideSleepWindow = true, gapsEmpty = true, nowMillis = at(6)
        )
        assertEquals(TodayWidgetState.NIGHT_WINDOW, state)
    }

    @Test
    fun morning_with_no_gaps_yet_is_quiet() {
        val state = resolveTodayWidgetState(
            hasUsageAccess = true, isInsideSleepWindow = false, gapsEmpty = true, nowMillis = at(8)
        )
        assertEquals(TodayWidgetState.MORNING_QUIET, state)
    }

    @Test
    fun morning_with_gaps_already_is_a_normal_day() {
        val state = resolveTodayWidgetState(
            hasUsageAccess = true, isInsideSleepWindow = false, gapsEmpty = false, nowMillis = at(8)
        )
        assertEquals(TodayWidgetState.DAY, state)
    }

    @Test
    fun afternoon_is_always_a_normal_day_even_with_no_gaps() {
        val state = resolveTodayWidgetState(
            hasUsageAccess = true, isInsideSleepWindow = false, gapsEmpty = true, nowMillis = at(15)
        )
        assertEquals(TodayWidgetState.DAY, state)
    }
}
