package com.serkodesign.tepera.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsBarColumnWidthsTest {

    @Test
    fun equalSplitWhenEveryWordFits() {
        assertEquals(listOf(80, 80, 80), statsBarColumnWidths(listOf(60, 55, 70), 240))
    }

    @Test
    fun columnWithLongWordGetsItsMinimumOthersShareTheRest() {
        // XZ1 Compact: "Розблокувань" ширше за рівну третину.
        assertEquals(listOf(81, 81, 84), statsBarColumnWidths(listOf(70, 55, 84), 246))
    }

    @Test
    fun fixingOneColumnCanPushAnotherOverItsShare() {
        assertEquals(listOf(40, 70, 90), statsBarColumnWidths(listOf(10, 70, 90), 200))
    }

    @Test
    fun fallsBackToEqualSplitWhenMinimumsDoNotFit() {
        assertEquals(listOf(50, 50), statsBarColumnWidths(listOf(80, 80), 100))
    }

    @Test
    fun emptyAndSingle() {
        assertEquals(emptyList<Int>(), statsBarColumnWidths(emptyList(), 100))
        assertEquals(listOf(100), statsBarColumnWidths(listOf(30), 100))
    }
}
