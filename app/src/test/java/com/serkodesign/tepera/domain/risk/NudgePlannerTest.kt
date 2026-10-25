package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NudgePlannerTest {
    // 2026-10-22 — четвер
    private fun at(day: Int, h: Int, m: Int) = LocalDateTime.of(2026, 10, day, h, m)
    private fun date(day: Int) = LocalDate.of(2026, 10, day)

    private fun window(
        id: String, start: Int, end: Int, dayType: DayType = DayType.ALL, enabled: Boolean = true, plan: Boolean = true
    ) = NudgePlanner.Candidate(id, start, end, dayType, enabled, plan)

    private val evening = window("e", 22 * 60 + 40, 10)
    private val lunch = window("l", 13 * 60, 13 * 60 + 30, DayType.WEEKDAYS)

    @Test
    fun `reminder fires ten minutes before the window`() {
        val n = NudgePlanner.next(listOf(evening), at(22, 12, 0), emptySet())!!
        assertEquals(at(22, 22, 30), n.fireAt)
        assertEquals(at(22, 22, 40), n.windowStart)
    }

    @Test
    fun `after the reminder time the next day is chosen`() {
        val n = NudgePlanner.next(listOf(evening), at(22, 22, 31), emptySet())!!
        assertEquals(at(23, 22, 30), n.fireAt)
    }

    @Test
    fun `windows without a plan or turned off are never nudged`() {
        assertNull(NudgePlanner.next(listOf(window("a", 600, 660, plan = false), window("b", 700, 760, enabled = false)), at(22, 0, 0), emptySet()))
    }

    @Test
    fun `a blocked day is skipped - at most one reminder per day`() {
        val n = NudgePlanner.next(listOf(evening), at(22, 12, 0), setOf(date(22)))!!
        assertEquals(at(23, 22, 30), n.fireAt)
    }

    @Test
    fun `earliest of several windows wins and the other is blocked after showing`() {
        val first = NudgePlanner.next(listOf(evening, lunch), at(22, 10, 0), emptySet())!!
        assertEquals("l", first.windowId)
        assertEquals(at(22, 12, 50), first.fireAt)
        // Після показу обіднього нагадування четвер заблоковано — вечірнє того ж дня не планується, наступне — п’ятничний обід.
        val second = NudgePlanner.next(listOf(evening, lunch), at(22, 13, 5), setOf(date(22)))!!
        assertEquals("l", second.windowId)
        assertEquals(at(23, 12, 50), second.fireAt)
    }

    @Test
    fun `weekday window skips the weekend`() {
        // Пʼятниця 23.10 після обіду: наступне нагадування — понеділок 26.10 о 12:50.
        val n = NudgePlanner.next(listOf(lunch), at(23, 14, 0), emptySet())!!
        assertEquals(at(26, 12, 50), n.fireAt)
    }

    @Test
    fun `reminder for a window just after midnight fires the evening before`() {
        val night = window("n", 5, 65, DayType.ALL) // 00:05–01:05 → нагадування 23:55
        val n = NudgePlanner.next(listOf(night), at(22, 12, 0), emptySet())!!
        assertEquals(at(22, 23, 55), n.fireAt)
        assertEquals(at(23, 0, 5), n.windowStart)
    }
}
