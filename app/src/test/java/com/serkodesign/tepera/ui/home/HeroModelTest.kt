package com.serkodesign.tepera.ui.home

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroModelTest {
    private val red = Color.Red
    private val green = Color.Green
    private val yellow = Color.Yellow
    private val gray = Color.Gray

    private fun act(name: String, minutes: Int) = HeroActivity(name, red, minutes)

    private fun data(activities: List<HeroActivity>, elapsed: Int = 410, online: Int = 130) =
        buildHeroState(true, 0L, elapsed, online, activities, DayStructure.Empty) as HeroUiState.Data

    @Test
    fun waterLevelIsOfflineShareOfElapsed() {
        // HANDOFF: 280 / 410 = 68%
        val state = data(emptyList(), elapsed = 410, online = 130)
        assertEquals(280, state.offlineMinutes)
        assertEquals(68, state.percent)
    }

    @Test
    fun onlineAboveElapsedNeverGivesNegativeOffline() {
        val state = data(emptyList(), elapsed = 100, online = 120)
        assertEquals(0, state.offlineMinutes)
        assertEquals(0f, state.waterLevel, 0f)
    }

    @Test
    fun stateByAccessAndElapsed() {
        val none = DayStructure.Empty
        assertTrue(buildHeroState(null, 0, 0, 0, emptyList(), none) is HeroUiState.Loading)
        assertTrue(buildHeroState(false, 0, 500, 0, emptyList(), none) is HeroUiState.NoAccess)
        assertTrue(buildHeroState(true, 0, 28, 0, emptyList(), none) is HeroUiState.Empty)
        assertTrue(buildHeroState(true, 0, 30, 0, emptyList(), none) is HeroUiState.Data)
    }

    @Test
    fun fourActivitiesAreAllShown() {
        val state = data(listOf(act("a", 10), act("b", 20), act("c", 30), act("d", 40)))
        assertEquals(false, state.isMany)
        assertEquals(listOf("d", "c", "b", "a"), state.visibleActivities.map { it.name })
    }

    @Test
    fun moreThanFourCollapsesToTopThreeSortedDescending() {
        val state = data((1..6).map { act("n$it", it * 10) })
        assertTrue(state.isMany)
        assertEquals(listOf("n6", "n5", "n4"), state.visibleActivities.map { it.name })
        assertEquals(3, state.hiddenActivities.size)
    }

    @Test
    fun atMostEightActivitiesAndZeroMinutesDropped() {
        val state = data((1..10).map { act("n$it", it * 5) } + act("zero", 0))
        assertEquals(8, state.activities.size)
        assertEquals(state.activities.sumOf { it.minutes }, state.namedMinutes)
    }

    // --- Смуга структури доби (принцип колишньої шкали "Твій день") ---------------------------------------

    private fun structure(
        online: Int = 0,
        categories: List<HeroActivity> = emptyList(),
        rest: Int = 0,
        length: Int,
        span: Int,
        target: Int? = null
    ) = buildDayStructure(online, categories, rest, length, span, target, onlineColor = yellow, restColor = green, futureColor = gray)

    @Test
    fun structureOrdersOnlineThenCategoriesThenRestThenHatchedFuture() {
        // довжина дня 240, до півночі 600: Online 60, дві категорії по 30, решта 120, майбутнє 360
        val s = structure(
            online = 60,
            categories = listOf(HeroActivity("a", red, 30), HeroActivity("b", Color.Blue, 30)),
            rest = 120, length = 240, span = 600
        )
        assertEquals(listOf(yellow, red, Color.Blue, green, gray), s.segments.map { it.color })
        assertEquals(listOf(false, false, false, false, true), s.segments.map { it.hatched })
        assertEquals(0f, s.segments.first().start, 1e-6f)
        assertEquals(0.1f, s.segments[0].end, 1e-4f) // 60 / 600
        assertEquals(0.4f, s.segments[3].end, 1e-4f) // 240 / 600 — межа "зараз"
        assertEquals(1f, s.segments.last().end, 1e-4f)
    }

    @Test
    fun overlappingSegmentsAreScaledToDayLength() {
        // сума 300 > довжини дня 240 → масштаб 0.8; майбутнє 360
        val s = structure(online = 100, rest = 200, length = 240, span = 600)
        // після масштабування 80 + 160 = 240, тож межа "зараз" лишається на 240 / 600
        assertEquals(0.4f, s.segments[1].end, 1e-4f)
        assertTrue(s.segments.last().hatched)
    }

    @Test
    fun noFutureSegmentWhenDayIsOver() {
        val s = structure(online = 100, rest = 140, length = 240, span = 240)
        assertTrue(s.segments.none { it.hatched })
        assertEquals(1f, s.segments.last().end, 1e-4f)
    }

    @Test
    fun zeroMinuteCategoriesAreSkippedAndEmptyInputGivesNoSegments() {
        val s = structure(categories = listOf(HeroActivity("a", red, 0)), length = 0, span = 0)
        assertTrue(s.segments.isEmpty())
        assertNull(s.targetFraction)
    }

    @Test
    fun targetFractionIsShareOfFullScaleNotOfElapsedDay() {
        // орієнтир 180 хв, повна шкала 600 → 0.3 незалежно від того, скільки дня вже минуло
        assertEquals(0.3f, structure(online = 10, length = 60, span = 600, target = 180).targetFraction!!, 1e-4f)
        assertEquals(0.3f, structure(online = 10, length = 400, span = 600, target = 180).targetFraction!!, 1e-4f)
    }

    @Test
    fun targetLargerThanSpanClampsToEndAndNullTargetGivesNoMarker() {
        assertEquals(1f, structure(online = 10, length = 60, span = 100, target = 500).targetFraction!!, 1e-4f)
        assertNull(structure(online = 10, length = 60, span = 100, target = null).targetFraction)
    }
}
