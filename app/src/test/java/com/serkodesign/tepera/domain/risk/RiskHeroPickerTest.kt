package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Test

class RiskHeroPickerTest {

    @Test
    fun windowInProgressBeatsEverything() {
        assertEquals(RiskHero.IN_WINDOW, RiskHeroPicker.pick(insideWindow = true, hasPauses = true, hasNextWindow = true, hasPattern = true))
    }

    @Test
    fun outsideWindowPausesComeFirst() {
        assertEquals(RiskHero.PAUSES, RiskHeroPicker.pick(false, hasPauses = true, hasNextWindow = true, hasPattern = true))
    }

    @Test
    fun thenNextWindowThenPattern() {
        assertEquals(RiskHero.NEXT_WINDOW, RiskHeroPicker.pick(false, false, hasNextWindow = true, hasPattern = true))
        assertEquals(RiskHero.PATTERN, RiskHeroPicker.pick(false, false, hasNextWindow = false, hasPattern = true))
    }

    @Test
    fun nothingToShow() {
        assertEquals(RiskHero.NONE, RiskHeroPicker.pick(false, false, false, false))
    }
}
