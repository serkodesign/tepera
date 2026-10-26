package com.serkodesign.tepera.domain.risk

import com.serkodesign.tepera.data.local.entity.RiskWindowEntity
import com.serkodesign.tepera.data.local.entity.WINDOW_PAUSE_MILLIS
import com.serkodesign.tepera.data.local.entity.isPausedAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowPauseTest {
    private fun window(pausedUntil: Long?) = RiskWindowEntity(
        id = "w", startMinute = 1360, endMinute = 10, dayType = DayType.ALL, recurrenceDays = 6, observedDays = 7,
        avgMinutes = 55, topApps = "", createdAt = 0, updatedAt = 0, isUserDisabled = false, pausedUntil = pausedUntil
    )

    @Test
    fun `not paused without date`() = assertFalse(window(null).isPausedAt(1_000L))

    @Test
    fun `paused before expiry and active after`() {
        val w = window(1_000L + WINDOW_PAUSE_MILLIS)
        assertTrue(w.isPausedAt(1_000L))
        assertTrue(w.isPausedAt(WINDOW_PAUSE_MILLIS))
        assertFalse(w.isPausedAt(1_000L + WINDOW_PAUSE_MILLIS))
    }

    @Test
    fun `pause lasts fourteen days`() = assertEquals(14L * 24 * 3_600_000L, WINDOW_PAUSE_MILLIS)
}
