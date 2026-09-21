package com.serkodesign.tepera.ui.stats

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.repository.GapCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayStatsTest {

    private val midnight = 1_000_000_000_000L
    private val slot = 30 * 60_000L

    @Test
    fun median_handles_odd_even_and_empty() {
        assertNull(medianOf(emptyList()))
        assertEquals(3, medianOf(listOf(5, 1, 3)))
        assertEquals(3, medianOf(listOf(2, 4, 1, 5))) // (2 + 4) / 2
    }

    @Test
    fun pause_only_fills_blank_slots_and_not_online_or_category() {
        val online = IntArray(48).also { it[20] = 10 }
        val entry = ActivityEntryEntity(
            categoryId = "c1", startTime = midnight + 21 * slot, durationMinutes = 30
        )
        // Пауза охоплює клітинки 19..22 (09:30-11:30): 20 — Online, 21 — запис, 19/22 — паузи.
        val gaps = listOf(GapCandidate(midnight + 19 * slot, 120))
        val timeline = buildDayTimeline(midnight, midnight, listOf(entry), online, gaps)

        assertEquals(TimelineSlot.Pause, timeline[19])
        assertEquals(TimelineSlot.Online, timeline[20])
        assertEquals(TimelineSlot.Category("c1"), timeline[21])
        assertEquals(TimelineSlot.Pause, timeline[22])
        assertEquals(TimelineSlot.Offline, timeline[23]) // день почався, нічого не залоговано
    }

    @Test
    fun without_online_data_unlogged_slots_stay_unknown() {
        val timeline = buildDayTimeline(midnight, midnight, emptyList(), IntArray(48), emptyList(), onlineKnown = false)
        assertEquals(TimelineSlot.Blank, timeline[5])
    }

    @Test
    fun slots_before_first_use_are_before_start() {
        val timeline = buildDayTimeline(midnight, midnight + 10 * slot, emptyList(), IntArray(48), emptyList())
        assertEquals(TimelineSlot.BeforeStart, timeline[0])
        assertEquals(TimelineSlot.BeforeStart, timeline[9])
        assertEquals(TimelineSlot.Offline, timeline[12])
    }
}

class OfflineUnionTest {

    private val minute = 60_000L
    private val t0 = 1_000_000_000_000L

    private fun span(fromMin: Int, toMin: Int) =
        com.serkodesign.tepera.util.TimeSpan(t0 + fromMin * minute, t0 + toMin * minute)

    private fun entry(fromMin: Int, durationMin: Int) = com.serkodesign.tepera.data.local.entity.ActivityEntryEntity(
        categoryId = "c", startTime = t0 + fromMin * minute, durationMinutes = durationMin
    )

    @Test
    fun overlaps_between_online_and_entries_are_counted_once() {
        // Вікно 10 год. Online 0-60, записи 30-120 і 90-150: разом зайнято 0-150 (150 хв), а не 60+90+60 = 210.
        val offline = offlineUnloggedMinutes(
            windowStart = t0, windowEnd = t0 + 600 * minute,
            onlineIntervals = listOf(span(0, 60)),
            entries = listOf(entry(30, 90), entry(90, 60)),
            sleepWindows = emptyList()
        )
        assertEquals(600 - 150, offline)
    }

    @Test
    fun window_without_anything_is_all_offline_and_never_negative() {
        assertEquals(120, offlineUnloggedMinutes(t0, t0 + 120 * minute, emptyList(), emptyList(), emptyList()))
        assertEquals(0, offlineUnloggedMinutes(t0, t0 + 60 * minute, listOf(span(0, 60)), listOf(entry(0, 90)), emptyList()))
    }

    @Test
    fun merge_spans_joins_overlapping_and_touching() {
        val merged = com.serkodesign.tepera.util.mergeTimeSpans(listOf(span(50, 80), span(0, 30), span(30, 40), span(60, 70)))
        assertEquals(listOf(span(0, 40), span(50, 80)), merged)
    }
}
