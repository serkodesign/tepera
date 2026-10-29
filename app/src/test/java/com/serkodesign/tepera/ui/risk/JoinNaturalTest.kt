package com.serkodesign.tepera.ui.risk

import org.junit.Assert.assertEquals
import org.junit.Test


class JoinNaturalTest {
    @Test
    fun `joins lists naturally`() {
        assertEquals("", joinNatural(emptyList(), " і "))
        assertEquals("a", joinNatural(listOf("a"), " і "))
        assertEquals("a і b", joinNatural(listOf("a", "b"), " і "))
        assertEquals("a, b і c", joinNatural(listOf("a", "b", "c"), " і "))
    }
}
