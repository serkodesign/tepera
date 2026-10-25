package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WindowMinutesAndSuggestionsTest {
    private val last = LocalDate.of(2026, 10, 20) // вівторок
    private val week = (6 downTo 0).map { last.minusDays(it.toLong()) }

    @Test
    fun `slot belongs to window by logical position`() {
        // Вікно 22:00–23:30 = слоти 88..93.
        assertTrue(WindowMinutes.slotInWindow(88, 22 * 60, 23 * 60 + 30))
        assertTrue(WindowMinutes.slotInWindow(93, 22 * 60, 23 * 60 + 30))
        assertFalse(WindowMinutes.slotInWindow(94, 22 * 60, 23 * 60 + 30))
        assertFalse(WindowMinutes.slotInWindow(87, 22 * 60, 23 * 60 + 30))
    }

    @Test
    fun `window across midnight includes morning slots`() {
        // 23:30–00:30 = слоти 94, 95, 0, 1.
        for (slot in listOf(94, 95, 0, 1)) assertTrue(WindowMinutes.slotInWindow(slot, 23 * 60 + 30, 30))
        assertFalse(WindowMinutes.slotInWindow(2, 23 * 60 + 30, 30))
    }

    @Test
    fun `average per day counts only slots inside the window and listed days`() {
        val records = week.map { SlotUsage(it, 88, 600) } + // 10 хв у вікні щодня
            SlotUsage(week[0], 50, 3600) + // поза вікном
            SlotUsage(last.minusDays(30), 88, 3600) // поза днями
        val avg = WindowMinutes.averagePerDay(records, week, 22 * 60, 23 * 60 + 30, DayType.ALL)
        assertEquals(10.0, avg!!, 0.001)
    }

    @Test
    fun `weekday window ignores weekends`() {
        // Лише будні мають по 30 хв; вихідні — 60 хв, але не рахуються.
        val records = week.map { SlotUsage(it, 52, if (it.dayOfWeek.value <= 5) 1800 else 3600) }
        val avg = WindowMinutes.averagePerDay(records, week, 13 * 60, 13 * 60 + 30, DayType.WEEKDAYS)
        assertEquals(30.0, avg!!, 0.001)
    }

    @Test
    fun `no days to count gives null`() {
        assertNull(WindowMinutes.averagePerDay(emptyList(), emptyList(), 0, 30, DayType.ALL))
        val saturday = listOf(LocalDate.of(2026, 10, 17))
        assertNull(WindowMinutes.averagePerDay(emptyList(), saturday, 0, 30, DayType.WEEKDAYS))
    }

    @Test
    fun `evening slots are 18 to 04`() {
        assertTrue(FeedSuggestions.isEveningSlot(72))
        assertTrue(FeedSuggestions.isEveningSlot(0))
        assertTrue(FeedSuggestions.isEveningSlot(15))
        assertFalse(FeedSuggestions.isEveningSlot(16))
        assertFalse(FeedSuggestions.isEveningSlot(71))
    }

    @Test
    fun `suggestions skip messengers and zero evening time and keep order`() {
        val top = FeedSuggestions.top(
            mapOf(
                "com.instagram.android" to 5000L,
                "org.telegram.messenger" to 9000L,
                "com.reddit.frontpage" to 7000L,
                "com.example.unused" to 0L
            )
        )
        assertEquals(listOf("com.reddit.frontpage", "com.instagram.android"), top)
    }
}
