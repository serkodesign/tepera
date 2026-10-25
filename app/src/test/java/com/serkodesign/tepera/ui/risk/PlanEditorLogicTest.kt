package com.serkodesign.tepera.ui.risk

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanEditorLogicTest {
    private val presets = listOf("a", "b", "c")

    @Test
    fun `no saved text selects nothing`() = assertEquals(-1, initialIndex(null, presets))

    @Test
    fun `saved text equal to a preset selects that chip`() = assertEquals(1, initialIndex("b", presets))

    @Test
    fun `saved text that is not a preset selects own variant`() = assertEquals(presets.size, initialIndex("my words", presets))
}
