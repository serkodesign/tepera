package com.serkodesign.tepera.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessStateProbeTest {
    @Test
    fun firesOnlyOnFalseToTrue() {
        assertTrue(becameTrue(previous = false, now = true))
        assertFalse(becameTrue(previous = true, now = true))
        assertFalse(becameTrue(previous = false, now = false))
        assertFalse(becameTrue(previous = true, now = false))
    }
}
