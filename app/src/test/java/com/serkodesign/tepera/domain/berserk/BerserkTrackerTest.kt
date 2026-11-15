package com.serkodesign.tepera.domain.berserk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BerserkTrackerTest {
    private val min = 60_000L
    private val tracker = BerserkTracker(breakMillis = 3 * min)

    @Test fun timeAccumulatesWhileActive() {
        tracker.enter("a", 0)
        assertEquals(10 * min, tracker.elapsed("a", 10 * min))
    }

    @Test fun shortBreakKeepsTheCount() {
        tracker.enter("a", 0)
        tracker.leave(10 * min)
        tracker.enter("a", 12 * min)
        assertEquals(10 * min + 3 * min, tracker.elapsed("a", 15 * min))
    }

    @Test fun longBreakResetsTheCount() {
        tracker.enter("a", 0)
        tracker.leave(10 * min)
        tracker.enter("a", 14 * min)
        assertEquals(min, tracker.elapsed("a", 15 * min))
    }

    @Test fun sharedKeyContinuesAcrossApps() {
        tracker.enter("*", 0)
        tracker.enter("*", 5 * min) // перехід в іншу стрічку у спільному режимі — той самий ключ
        assertEquals(8 * min, tracker.elapsed("*", 8 * min))
    }

    @Test fun perAppKeysAreCountedApart() {
        tracker.enter("a", 0)
        tracker.enter("b", 5 * min)
        assertEquals(5 * min, tracker.elapsed("a", 6 * min))
        assertEquals(min, tracker.elapsed("b", 6 * min))
        assertFalse(tracker.isActive("a"))
        assertTrue(tracker.isActive("b"))
    }

    @Test fun resetStartsFromZero() {
        tracker.enter("a", 0)
        tracker.reset("a", 30 * min)
        assertEquals(2 * min, tracker.elapsed("a", 32 * min))
    }
}
