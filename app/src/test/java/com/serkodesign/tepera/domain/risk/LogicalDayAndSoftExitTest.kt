package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LogicalDayAndSoftExitTest {
    private val zone = ZoneId.of("Europe/Kyiv")
    private fun millis(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `evening across midnight is one logical day`() {
        assertEquals(LogicalDay.epochDay(millis(2026, 10, 8, 23, 30), zone), LogicalDay.epochDay(millis(2026, 10, 9, 0, 30), zone))
    }

    @Test
    fun `logical day rolls over at 04 00`() {
        assertEquals(LogicalDay.epochDay(millis(2026, 10, 9, 3, 59), zone), LogicalDay.epochDay(millis(2026, 10, 8, 22, 0), zone))
        assertNotEquals(LogicalDay.epochDay(millis(2026, 10, 9, 4, 0), zone), LogicalDay.epochDay(millis(2026, 10, 9, 3, 59), zone))
    }

    @Test
    fun `logical day survives winter time change`() {
        // 25.10.2026 доба має 25 год: 22:40 і 00:10 наступної все одно одна логічна доба.
        assertEquals(LogicalDay.epochDay(millis(2026, 10, 24, 22, 40), zone), LogicalDay.epochDay(millis(2026, 10, 25, 0, 10), zone))
    }

    @Test
    fun `soft exit is on time up to two minutes after deadline`() {
        val deadline = 1_000_000L
        assertTrue(SoftExitTiming.isOnTime(deadline, deadline - 60_000))
        assertTrue(SoftExitTiming.isOnTime(deadline, deadline + 2 * 60_000L))
        assertFalse(SoftExitTiming.isOnTime(deadline, deadline + 2 * 60_000L + 1))
    }
}
