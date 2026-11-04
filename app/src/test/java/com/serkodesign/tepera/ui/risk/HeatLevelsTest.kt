package com.serkodesign.tepera.ui.risk

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatLevelsTest {
    @Test
    fun `alpha table from the plan`() {
        assertEquals(0, HeatLevels.levelForAlpha(null))
        assertEquals(1, HeatLevels.levelForAlpha(0.15f))
        assertEquals(1, HeatLevels.levelForAlpha(0.19f))
        assertEquals(2, HeatLevels.levelForAlpha(0.20f))
        assertEquals(2, HeatLevels.levelForAlpha(0.30f))
        assertEquals(3, HeatLevels.levelForAlpha(0.31f))
        assertEquals(3, HeatLevels.levelForAlpha(0.60f))
        assertEquals(4, HeatLevels.levelForAlpha(0.61f))
        assertEquals(4, HeatLevels.levelForAlpha(0.90f))
        assertEquals(5, HeatLevels.levelForAlpha(1.0f))
    }

    @Test
    fun `buckets map to levels through the legacy alphas`() {
        assertEquals(0, HeatLevels.levelForBucket(0))
        assertEquals(1, HeatLevels.levelForBucket(1)) // 0.15
        assertEquals(3, HeatLevels.levelForBucket(2)) // 0.45
        assertEquals(4, HeatLevels.levelForBucket(3)) // 0.7
        assertEquals(5, HeatLevels.levelForBucket(4)) // 1.0
    }

    @Test
    fun `levels grow with the bucket`() {
        val levels = (0..4).map(HeatLevels::levelForBucket)
        assertEquals(levels.sorted(), levels)
        assertEquals(listOf(1, 3, 4, 5), HeatLevels.USED_LEVELS)
    }

    @Test
    fun `out of range bucket is clamped`() {
        assertEquals(0, HeatLevels.levelForBucket(-3))
        assertEquals(5, HeatLevels.levelForBucket(9))
    }
}
