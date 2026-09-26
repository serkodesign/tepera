package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WelcomeBackTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private val day = 24L * 60 * 60 * 1000

    private fun at(d: Int, h: Int = 12): Long =
        LocalDateTime.of(2026, 10, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun gap_needsThreeFullDays_andAKnownLastOpen() {
        assertFalse(WelcomeBack.gapReached(0, at(10)))
        assertFalse(WelcomeBack.gapReached(at(1), at(3, 23)))
        assertTrue(WelcomeBack.gapReached(at(1), at(4)))
        assertTrue(WelcomeBack.gapReached(at(1), at(20))) // 7+ днів
    }

    @Test
    fun periodDays_areFullDaysBetweenLastOpenDayAndToday() {
        val days = WelcomeBack.periodDays(at(1, 22), at(5, 9), zone)
        assertEquals(listOf(2, 3, 4), days.map { it.dayOfMonth })
    }

    @Test
    fun summary_sumsLoggedMinutes_andAveragesOnlineOverKnownDays() {
        val days = WelcomeBack.periodDays(at(1), at(6), zone) // 2,3,4,5
        val online = mapOf(days[0].toEpochDay() to 120, days[1].toEpochDay() to 180) // два дні з даними
        val entries = listOf(
            EntrySpan(at(2, 10), 30),
            EntrySpan(at(4, 18), 45),
            EntrySpan(at(1, 10), 500), // день останнього відкриття — не входить
            EntrySpan(at(6, 8), 500) // сьогодні — не входить
        )
        val summary = WelcomeBackSummary.build(days, online, entries, zone)!!
        assertEquals(4, summary.days)
        assertEquals(75, summary.loggedMinutes)
        assertEquals(150, summary.averageOnlineMinutes)
    }

    @Test
    fun summary_isNullWithoutTwoDays_orWithoutAnyNumber() {
        val twoDays = WelcomeBack.periodDays(at(1), at(4), zone)
        assertNull(WelcomeBackSummary.build(twoDays.take(1), mapOf(1L to 60), emptyList(), zone))
        assertNull(WelcomeBackSummary.build(twoDays, emptyMap(), emptyList(), zone))
        assertNotNull(WelcomeBackSummary.build(twoDays, mapOf(twoDays[0].toEpochDay() to 60), emptyList(), zone))
    }

    @Test
    fun onlineOnly_stillShows_withZeroLogged() {
        val days = WelcomeBack.periodDays(at(1), at(5), zone)
        val summary = WelcomeBackSummary.build(days, mapOf(days[0].toEpochDay() to 90), emptyList(), zone)!!
        assertEquals(0, summary.loggedMinutes)
        assertEquals(90, summary.averageOnlineMinutes)
    }

    @Test
    fun coverage_keepsOnlyDaysStartingAfterEarliestEvent() {
        val days = WelcomeBack.periodDays(at(1), at(10), zone) // 2..9
        val earliest = at(4, 0) + 1 // історія починається трохи після півночі 4-го
        assertEquals(listOf(5, 6, 7, 8, 9), HistoryCoverage.coveredDays(days, earliest, zone).map { it.dayOfMonth })
    }

    @Test
    fun coverage_isEmptyWithoutHistory() {
        val days = WelcomeBack.periodDays(at(1), at(10), zone)
        assertEquals(emptyList<LocalDate>(), HistoryCoverage.coveredDays(days, null, zone))
    }
}
