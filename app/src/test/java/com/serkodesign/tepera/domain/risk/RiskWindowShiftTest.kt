package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RiskWindowShiftTest {
    private fun window(start: Int, end: Int) = RiskCandidate(start, end, 6, 7, 30, emptyList())
    private val stored = window(22 * 60, 23 * 60 + 30)

    @Test
    fun `start shifted later two weeks in a row suggests update`() {
        val week1 = window(22 * 60 + 30, 24 * 60 - 0).copy(endMinute = 0)
        val week2 = window(22 * 60 + 40, 60)
        val suggestion = RiskWindowFinder.suggestShift(stored, listOf(listOf(week1), listOf(week2)))
        assertEquals(week2, suggestion)
    }

    @Test
    fun `one week of shift is not enough`() {
        val same = window(22 * 60 + 10, 23 * 60 + 30)
        val shifted = window(22 * 60 + 30, 23 * 60 + 50)
        assertNull(RiskWindowFinder.suggestShift(stored, listOf(listOf(same), listOf(shifted))))
    }

    @Test
    fun `shifts in opposite directions do not count`() {
        val later = window(22 * 60 + 30, 23 * 60 + 59)
        val earlier = window(21 * 60 + 30, 23 * 60 + 10)
        assertNull(RiskWindowFinder.suggestShift(stored, listOf(listOf(later), listOf(earlier))))
    }

    @Test
    fun `week without matching window gives nothing`() {
        val unrelated = window(13 * 60, 13 * 60 + 30)
        val shifted = window(22 * 60 + 30, 23 * 60 + 59)
        assertNull(RiskWindowFinder.suggestShift(stored, listOf(listOf(unrelated), listOf(shifted))))
    }

    @Test
    fun `shift across midnight is measured in the logical day`() {
        val late = window(23 * 60 + 30, 60)
        val shiftedLate = window(23 * 60 + 55, 90)
        val shiftedMore = window(0, 120)
        val result = RiskWindowFinder.suggestShift(late, listOf(listOf(shiftedLate), listOf(shiftedMore)))
        assertEquals(shiftedMore, result)
    }
}
