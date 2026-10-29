package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class PlanOverlayRuleTest {
    private val evening = NextWindow.Spec("w1", 22 * 60 + 40, 24 * 60 + 10, DayType.ALL)

    private fun pick(
        now: LocalDateTime,
        enabled: Boolean = true,
        lastShown: Long = 0L,
        nowMillis: Long = 10_000_000_000L
    ) = PlanOverlayRule.pick(listOf(PlanOverlayRule.Candidate(evening, enabled)), now, nowMillis, { lastShown })

    @Test
    fun `inside an enabled window shows`() {
        assertEquals("w1", pick(LocalDateTime.of(2026, 10, 26, 23, 0)))
    }

    @Test
    fun `before window does not show`() {
        assertNull(pick(LocalDateTime.of(2026, 10, 26, 22, 0)))
    }

    @Test
    fun `window across midnight still shows after midnight`() {
        assertEquals("w1", pick(LocalDateTime.of(2026, 10, 27, 0, 5)))
    }

    @Test
    fun `disabled window does not show`() {
        assertNull(pick(LocalDateTime.of(2026, 10, 26, 23, 0), enabled = false))
    }

    @Test
    fun `recently shown idea is not repeated until debounce passes`() {
        val now = 10_000_000_000L
        assertNull(pick(LocalDateTime.of(2026, 10, 26, 23, 0), lastShown = now - 5 * 60_000L, nowMillis = now))
        assertEquals("w1", pick(LocalDateTime.of(2026, 10, 26, 23, 0), lastShown = now - 25 * 60_000L, nowMillis = now))
    }
}
