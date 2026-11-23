package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Фаза C плану first-day-value: для пошуку вікон досить 5 покритих діб із 7, а покриті доби повертаються для базової лінії. */
class ObservedDaysThresholdTest {
    private val last = LocalDate.of(2026, 10, 20)
    private val week = (6 downTo 0).map { last.minusDays(it.toLong()) }

    /** Покриті [n] найновіших діб (разом із наступним календарним днем кожної, як вимагає логічна доба). */
    private fun coveredNewest(n: Int): Set<LocalDate> = week.takeLast(n).flatMap { listOf(it, it.plusDays(1)) }.toSet()

    @Test
    fun `default needs five covered days`() {
        assertEquals(5, RiskConfig().minObservedDays)
    }

    @Test
    fun `five covered days are enough`() {
        val r = RiskWindowFinder.find(emptyList(), coveredNewest(5), last)
        assertTrue(r.enoughData)
        assertEquals(5, r.observedDays)
        assertEquals(week.takeLast(5), r.coveredDays)
    }

    @Test
    fun `four covered days are not enough but still reported`() {
        val r = RiskWindowFinder.find(emptyList(), coveredNewest(4), last)
        assertFalse(r.enoughData)
        assertEquals(week.takeLast(4), r.coveredDays)
    }
}
