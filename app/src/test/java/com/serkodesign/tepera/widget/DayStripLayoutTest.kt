package com.serkodesign.tepera.widget

import com.serkodesign.tepera.util.TimeSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** W-3: `buildDayStripLayout()` — чиста функція, DaySnapshot → нормалізовані частки `[0,1]`. */
class DayStripLayoutTest {

    private fun snapshot(
        dayStart: Long,
        dayEnd: Long,
        gaps: List<NamedGap> = emptyList(),
        onlineIntervals: List<TimeSpan> = emptyList()
    ) = DaySnapshot(dayStart, dayEnd, gaps, phoneFreeMinutes = 0, longestGap = null, namedMinutesByCategory = emptyMap(), onlineIntervals = onlineIntervals)

    @Test
    fun gap_in_the_middle_of_the_day_maps_to_the_correct_fraction() {
        // Доба 0..1000, пауза 250..500 -> [0.25, 0.5).
        val snap = snapshot(0L, 1000L, gaps = listOf(NamedGap(250L, 500L, "reading")))
        val layout = buildDayStripLayout(snap)

        assertEquals(1, layout.gapSegments.size)
        val segment = layout.gapSegments.single()
        assertEquals(0.25f, segment.startFraction, 0.0001f)
        assertEquals(0.5f, segment.endFraction, 0.0001f)
        assertEquals("reading", segment.categoryId)
    }

    @Test
    fun unnamed_gap_keeps_null_category() {
        val snap = snapshot(0L, 1000L, gaps = listOf(NamedGap(0L, 100L, null)))
        assertEquals(null, buildDayStripLayout(snap).gapSegments.single().categoryId)
    }

    @Test
    fun online_interval_becomes_a_usage_tick_at_the_right_position() {
        val snap = snapshot(0L, 1000L, onlineIntervals = listOf(TimeSpan(800L, 900L)))
        val tick = buildDayStripLayout(snap).usageTicks.single()

        assertEquals(0.8f, tick.startFraction, 0.0001f)
        assertEquals(0.9f, tick.endFraction, 0.0001f)
    }

    @Test
    fun gap_starting_before_day_start_is_clipped_to_zero() {
        val snap = snapshot(1000L, 2000L, gaps = listOf(NamedGap(500L, 1500L, "nature")))
        val segment = buildDayStripLayout(snap).gapSegments.single()

        assertEquals(0f, segment.startFraction, 0.0001f)
        assertEquals(0.5f, segment.endFraction, 0.0001f)
    }

    @Test
    fun gap_ending_after_day_end_is_clipped_to_one() {
        val snap = snapshot(0L, 1000L, gaps = listOf(NamedGap(800L, 1500L, "nature")))
        val segment = buildDayStripLayout(snap).gapSegments.single()

        assertEquals(0.8f, segment.startFraction, 0.0001f)
        assertEquals(1f, segment.endFraction, 0.0001f)
    }

    @Test
    fun gap_entirely_outside_the_day_is_dropped() {
        val snap = snapshot(1000L, 2000L, gaps = listOf(NamedGap(0L, 500L, "nature")))
        assertTrue(buildDayStripLayout(snap).gapSegments.isEmpty())
    }

    @Test
    fun zero_length_day_yields_an_empty_layout_without_crashing() {
        val snap = snapshot(1000L, 1000L, gaps = listOf(NamedGap(1000L, 1100L, "reading")))
        val layout = buildDayStripLayout(snap)

        assertTrue(layout.gapSegments.isEmpty())
        assertTrue(layout.usageTicks.isEmpty())
    }

    @Test
    fun multiple_gaps_and_ticks_are_all_preserved_in_order() {
        val snap = snapshot(
            0L, 1000L,
            gaps = listOf(NamedGap(100L, 200L, "reading"), NamedGap(600L, 700L, null)),
            onlineIntervals = listOf(TimeSpan(300L, 350L), TimeSpan(800L, 850L))
        )
        val layout = buildDayStripLayout(snap)

        assertEquals(2, layout.gapSegments.size)
        assertEquals(2, layout.usageTicks.size)
        assertEquals(0.1f, layout.gapSegments[0].startFraction, 0.0001f)
        assertEquals(0.6f, layout.gapSegments[1].startFraction, 0.0001f)
    }
}
