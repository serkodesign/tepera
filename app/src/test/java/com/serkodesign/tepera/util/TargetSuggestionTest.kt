package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetSuggestionTest {

    @Test
    fun average_isPlainMeanOverDaysWithData_andNullWithoutDays() {
        assertEquals(180, TargetSuggestion.average(listOf(120, 180, 240)))
        assertNull(TargetSuggestion.average(emptyList()))
    }

    @Test
    fun hours_roundToNearestHour_withinSliderRange() {
        assertEquals(3, TargetSuggestion.hoursFor(170)) // 2.83 год
        assertEquals(2, TargetSuggestion.hoursFor(140)) // 2.33 год
        assertEquals(3, TargetSuggestion.hoursFor(150)) // 2.5 → 3
    }

    @Test
    fun hours_areClampedToOneToEight() {
        assertEquals(1, TargetSuggestion.hoursFor(10))
        assertEquals(1, TargetSuggestion.hoursFor(0))
        assertEquals(8, TargetSuggestion.hoursFor(11 * 60))
    }
}
