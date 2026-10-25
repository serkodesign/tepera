package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class NextWindowTest {
    private val evening = NextWindow.Spec("e", 22 * 60 + 40, 10, DayType.ALL)          // 22:40–00:10
    private val lunch = NextWindow.Spec("l", 13 * 60, 13 * 60 + 30, DayType.WEEKDAYS) // 13:00–13:30
    // 2026-10-22 — четвер
    private fun at(day: Int, h: Int, m: Int) = LocalDateTime.of(2026, 10, day, h, m)

    @Test
    fun `upcoming window starts later today`() {
        val o = NextWindow.occurrences(listOf(evening), at(22, 20, 30)).single()
        assertEquals(at(22, 22, 40), o.startAt)
        assertEquals(at(23, 0, 10), o.endAt)
        assertFalse(o.isInside(at(22, 20, 30)))
    }

    @Test
    fun `window across midnight is inside before midnight and after it`() {
        val before = NextWindow.occurrences(listOf(evening), at(22, 23, 30)).single()
        assertTrue(before.isInside(at(22, 23, 30)))
        assertEquals(at(23, 0, 10), before.endAt)

        val after = NextWindow.occurrences(listOf(evening), at(23, 0, 5)).single()
        assertTrue(after.isInside(at(23, 0, 5)))
        assertEquals(at(22, 22, 40), after.startAt) // почалось учора
    }

    @Test
    fun `after the window ended the next one is tomorrow`() {
        val o = NextWindow.occurrences(listOf(evening), at(23, 0, 11)).single()
        assertEquals(at(23, 22, 40), o.startAt)
    }

    @Test
    fun `weekday window skips the weekend`() {
        // П’ятниця 23.10 о 14:00 — обід минув; наступний у понеділок 26.10.
        val o = NextWindow.occurrences(listOf(lunch), at(23, 14, 0)).single()
        assertEquals(at(26, 13, 0), o.startAt)
    }

    @Test
    fun `results are ordered by start with the running window first`() {
        val list = NextWindow.occurrences(listOf(evening, lunch), at(22, 12, 0))
        assertEquals(listOf("l", "e"), list.map { it.id })
        val running = NextWindow.occurrences(listOf(evening, lunch), at(22, 13, 10))
        assertEquals("l", running.first().id)
        assertTrue(running.first().isInside(at(22, 13, 10)))
    }

    @Test
    fun `weekday window starting after midnight belongs to the previous day`() {
        // 00:30–01:30 вікно «лише будні»: у ніч на суботу (логічна доба — пʼятниця) воно діє, у ніч на понеділок (неділя) — ні.
        val night = NextWindow.Spec("n", 30, 90, DayType.WEEKDAYS)
        val sat = NextWindow.occurrences(listOf(night), at(24, 0, 10)).single() // субота 00:10
        assertEquals(at(24, 0, 30), sat.startAt)
        val sunNight = NextWindow.occurrences(listOf(night), at(25, 12, 0)).single() // неділя вдень
        assertEquals(at(27, 0, 30), sunNight.startAt) // понеділок 00:30 належить неділі → пропущено; вівторок 00:30 — понеділку
        assertEquals(java.time.DayOfWeek.TUESDAY, sunNight.startAt.dayOfWeek)
    }
}
