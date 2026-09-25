package com.serkodesign.tepera.util

import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

class GateTextsTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private fun at(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, 5, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun bag_showsEveryTextOnce_thenReshuffles() {
        val random = Random(1)
        var remaining = emptyList<Int>()
        val firstRound = mutableListOf<Int>()
        repeat(GateTexts.TOTAL) {
            val pick = GateTexts.pick(remaining, isNight = true, random = random)
            firstRound += pick.index
            remaining = pick.remaining
        }
        assertEquals((0 until GateTexts.TOTAL).toSet(), firstRound.toSet()) // усі 27 різні
        assertEquals(GateTexts.TOTAL, firstRound.size)
        assertTrue(remaining.isEmpty())
        // 28-й показ — нове коло: мішок наповнено заново.
        val next = GateTexts.pick(remaining, isNight = true, random = random)
        assertEquals(GateTexts.TOTAL - 1, next.remaining.size)
    }

    @Test
    fun daytime_neverPicksNightTexts() {
        val random = Random(7)
        var remaining = emptyList<Int>()
        repeat(200) {
            val pick = GateTexts.pick(remaining, isNight = false, random = random)
            assertFalse(pick.index in GateTexts.NIGHT_INDICES)
            remaining = pick.remaining
        }
    }

    @Test
    fun daytime_withOnlyNightTextsLeft_refillsInsteadOfShowingNight() {
        val pick = GateTexts.pick(listOf(24, 25, 26), isNight = false, random = Random(3))
        assertFalse(pick.index in GateTexts.NIGHT_INDICES)
        assertEquals(GateTexts.TOTAL - 1, pick.remaining.size)
    }

    @Test
    fun night_canPickNightTexts() {
        val pick = GateTexts.pick(listOf(25), isNight = true, random = Random(1))
        assertEquals(25, pick.index)
        assertTrue(pick.remaining.isEmpty())
    }

    @Test
    fun night_withoutSleepWindow_is22to06() {
        assertTrue(GateTexts.isNight(at(22), emptyList(), zone))
        assertTrue(GateTexts.isNight(at(3), emptyList(), zone))
        assertFalse(GateTexts.isNight(at(6), emptyList(), zone))
        assertFalse(GateTexts.isNight(at(15), emptyList(), zone))
    }

    @Test
    fun night_withSleepWindow_usesTheWindow() {
        val window = SleepWindowEntity(slot = 1, startMinuteOfDay = 9 * 60, endMinuteOfDay = 16 * 60, enabled = true)
        assertTrue(GateTexts.isNight(at(12), listOf(window), zone)) // нічна зміна: сплять удень
        assertFalse(GateTexts.isNight(at(23), listOf(window), zone)) // вікна сну немає — не «ніч»
    }

    @Test
    fun encodeDecode_roundTrips_andGarbageIsEmpty() {
        assertEquals(listOf(3, 9, 26), GateTexts.decode(GateTexts.encode(listOf(3, 9, 26))))
        assertEquals(emptyList<Int>(), GateTexts.decode("abc,,"))
        assertEquals(emptyList<Int>(), GateTexts.decode(null))
    }

    @Test
    fun growingDelay_addsFiveSecondsPerRepeat_capsAtTwenty() {
        assertEquals(5, GrowingDelay.effectiveSeconds(5, 0))
        assertEquals(10, GrowingDelay.effectiveSeconds(5, 1))
        assertEquals(15, GrowingDelay.effectiveSeconds(5, 2))
        assertEquals(20, GrowingDelay.effectiveSeconds(5, 3))
        assertEquals(20, GrowingDelay.effectiveSeconds(5, 10))
        assertEquals(20, GrowingDelay.effectiveSeconds(10, 5))
    }

    @Test
    fun growingDelay_resetsAfterThirtyMinutesWithoutOpenings() {
        val t0 = at(12)
        val within = t0 + 29 * 60 * 1000
        val after = t0 + GrowingDelay.WINDOW_MILLIS + 1
        assertEquals(0, GrowingDelay.repeatsAfterShow(0, 0, t0)) // перше відкриття
        assertEquals(1, GrowingDelay.repeatsAfterShow(0, t0, within))
        assertEquals(3, GrowingDelay.repeatsAfterShow(2, t0, within))
        assertEquals(0, GrowingDelay.repeatsAfterShow(2, t0, after)) // скидання
    }
}
