package com.serkodesign.tepera.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Тема «За часом»: ступені «Поточного» настрою й плавність «Світанку» (без пристрою й без зміни годинника). */
class ThemeByTimeTest {
    private fun m(h: Int, min: Int = 0) = h * 60 + min

    @Test
    fun classicPhases_dayEveningNight() {
        assertEquals(TimePhase.NIGHT, timePhaseAt(m(2)))
        assertEquals(TimePhase.NIGHT, timePhaseAt(m(4, 59)))
        assertEquals(TimePhase.DAY, timePhaseAt(m(5)))
        assertEquals(TimePhase.DAY, timePhaseAt(m(18, 59)))
        assertEquals(TimePhase.EVENING, timePhaseAt(m(19)))
        assertEquals(TimePhase.EVENING, timePhaseAt(m(22, 59)))
        assertEquals(TimePhase.NIGHT, timePhaseAt(m(23)))
    }

    @Test
    fun classicColors_threeGradations_nightDarkestOfAll() {
        val day = classicTimeColors(m(12))
        val evening = classicTimeColors(m(21))
        val night = classicTimeColors(m(2))
        assertFalse(day.isDark)
        assertTrue(evening.isDark)
        assertTrue(night.isDark)
        assertTrue(night.backgroundTop!!.luminance() < evening.backgroundTop!!.luminance())
        assertTrue(evening.backgroundTop!!.luminance() < day.backgroundTop!!.luminance())
    }

    @Test
    fun dawn_lightByDay_darkInEvening_darkestAtNight() {
        val noon = dawnColorsForMinute(m(12))
        val evening = dawnColorsForMinute(m(21))
        val night = dawnColorsForMinute(m(2))
        assertFalse(noon.isDark)
        assertTrue(evening.isDark)
        assertTrue(night.isDark)
        fun avg(c: List<Color>) = c.map { it.luminance() }.average()
        assertTrue(avg(night.backgroundStops!!) < avg(evening.backgroundStops!!))
        assertTrue(avg(evening.backgroundStops!!) < avg(noon.backgroundStops!!))
    }

    @Test
    fun dawn_skyChangesSmoothly_noJumpsBetweenFiveMinuteSteps() {
        // Між сусідніми кроками по 5 хв жоден канал жодного кольору неба не стрибає більш ніж на 0.12
        // (найшвидше — вечірнє потемніння за 45 хв: ≈0.114 за крок).
        var prev = dawnColorsForMinute(0).backgroundStops!!
        for (minute in 5 until 24 * 60 step 5) {
            val cur = dawnColorsForMinute(minute).backgroundStops!!
            cur.indices.forEach { i ->
                val d = maxOf(
                    kotlin.math.abs(cur[i].red - prev[i].red),
                    kotlin.math.abs(cur[i].green - prev[i].green),
                    kotlin.math.abs(cur[i].blue - prev[i].blue)
                )
                assertTrue("стрибок неба $d о $minute хв", d < 0.12f)
            }
            prev = cur
        }
    }

    @Test
    fun eveningShift_peaksAtWinterSolstice_zeroInSummer_invertedInSouth() {
        fun d(month: Int, day: Int) = java.time.LocalDate.of(2026, month, day)
        assertEquals(MAX_EVENING_SHIFT_MINUTES, eveningShiftMinutes(d(12, 21), southern = false))
        assertEquals(0, eveningShiftMinutes(d(6, 21), southern = false))
        assertEquals(0, eveningShiftMinutes(d(7, 15), southern = false))
        // Південна півкуля — навпаки: максимум у червні, нуль у грудні.
        assertEquals(MAX_EVENING_SHIFT_MINUTES, eveningShiftMinutes(d(6, 21), southern = true))
        assertEquals(0, eveningShiftMinutes(d(12, 21), southern = true))
        // Листопад — уже помітно раніше, але ще не максимум; ріст і спад плавні (без стрибків між днями).
        assertTrue(eveningShiftMinutes(d(11, 6), southern = false) in 60..110)
        var prev = eveningShiftMinutes(java.time.LocalDate.of(2026, 1, 1), southern = false)
        var date = java.time.LocalDate.of(2026, 1, 2)
        while (date.year == 2026) {
            val cur = eveningShiftMinutes(date, southern = false)
            assertTrue("стрибок зсуву ${cur - prev} хв $date", kotlin.math.abs(cur - prev) <= 6)
            prev = cur
            date = date.plusDays(1)
        }
    }

    @Test
    fun classicEveningStartsEarlierWithShift_nightStartUnchanged() {
        assertEquals(TimePhase.DAY, timePhaseAt(m(16, 59), shift = 120))
        assertEquals(TimePhase.EVENING, timePhaseAt(m(17), shift = 120))
        assertEquals(TimePhase.DAY, timePhaseAt(m(17), shift = 0))
        assertEquals(TimePhase.NIGHT, timePhaseAt(m(23), shift = 120))
        assertEquals(TimePhase.EVENING, timePhaseAt(m(22, 59), shift = 120))
    }

    @Test
    fun dawnDarkensEarlierWithShift_andStaysSmooth() {
        assertFalse(dawnColorsForMinute(m(17, 30), shift = 0).isDark)
        assertTrue(dawnColorsForMinute(m(17, 30), shift = 120).isDark)
        var prev = dawnColorsForMinute(0, shift = 120).backgroundStops!!
        for (minute in 5 until 24 * 60 step 5) {
            val cur = dawnColorsForMinute(minute, shift = 120).backgroundStops!!
            cur.indices.forEach { i ->
                val dd = maxOf(
                    kotlin.math.abs(cur[i].red - prev[i].red),
                    kotlin.math.abs(cur[i].green - prev[i].green),
                    kotlin.math.abs(cur[i].blue - prev[i].blue)
                )
                assertTrue("стрибок неба $dd о $minute хв", dd < 0.12f)
            }
            prev = cur
        }
    }

    @Test
    fun dawn_dayEndsAsEveningStarts_midnightWrapsToNight() {
        // Кінець доби збігається з початком: 23:55 і 00:00 — однаково нічні.
        val a = dawnColorsForMinute(m(23, 55)).backgroundStops!!
        val b = dawnColorsForMinute(m(0)).backgroundStops!!
        assertEquals(a, b)
    }
}
