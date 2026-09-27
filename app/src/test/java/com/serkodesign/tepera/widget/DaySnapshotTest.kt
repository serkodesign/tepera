package com.serkodesign.tepera.widget

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.data.repository.GapCandidate
import com.serkodesign.tepera.util.TimeSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** W-1: `buildDaySnapshot()` — чиста функція, ті самі 5 сценаріїв, що вимагає завдання по віджетах. */
class DaySnapshotTest {

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, day, hour, minute, 0)
        }.timeInMillis

    private fun sleepWindow(startHour: Int, endHour: Int) =
        SleepWindowEntity(slot = 1, startMinuteOfDay = startHour * 60, endMinuteOfDay = endHour * 60, enabled = true)

    @Test
    fun day_crossing_midnight_keeps_gap_end_and_duration_correct() {
        // Вікно дня 22:00 (14-те) - 08:00 (15-те) — саме перетинає північ.
        val dayStart = at(14, 22)
        val dayEnd = at(15, 8)
        // Пауза 23:30 -> 01:00 наступної доби (90 хв) — теж перетинає північ.
        val gap = GapCandidate(startTime = at(14, 23, 30), durationMinutes = 90)

        val snapshot = buildDaySnapshot(dayStart, dayEnd, listOf(gap), emptyList(), emptyList(), emptyList())

        assertEquals(1, snapshot.gaps.size)
        assertEquals(at(15, 1, 0), snapshot.gaps.single().endMillis)
        assertEquals(90, snapshot.gaps.single().durationMinutes)
        assertEquals(snapshot.gaps.single(), snapshot.longestGap)
    }

    @Test
    fun sleep_window_reduces_phone_free_minutes() {
        val dayStart = at(14, 20)
        val dayEnd = at(15, 8) // 12 годин без будь-якого Online чи запису
        val withoutSleepWindow = buildDaySnapshot(dayStart, dayEnd, emptyList(), emptyList(), emptyList(), emptyList())
        val withSleepWindow = buildDaySnapshot(
            dayStart, dayEnd, emptyList(), emptyList(), emptyList(),
            listOf(sleepWindow(0, 6)) // 6 год вікна сну всередині проміжку
        )

        assertEquals(12 * 60, withoutSleepWindow.phoneFreeMinutes)
        assertEquals(6 * 60, withSleepWindow.phoneFreeMinutes) // 12 год мінус 6 год сну
    }

    @Test
    fun day_with_zero_gaps_has_no_longest_gap_and_empty_category_summary() {
        val snapshot = buildDaySnapshot(at(14, 8), at(15, 0), emptyList(), emptyList(), emptyList(), emptyList())

        assertTrue(snapshot.gaps.isEmpty())
        assertNull(snapshot.longestGap)
        assertTrue(snapshot.namedMinutesByCategory.isEmpty())
    }

    @Test
    fun revoked_usage_access_still_produces_a_valid_snapshot() {
        // Те саме, що передає DaySnapshotRepository, коли hasUsageAccess() == false: усе порожнє.
        val dayStart = at(14, 8)
        val dayEnd = at(14, 20) // 12 год без вікон сну
        val snapshot = buildDaySnapshot(dayStart, dayEnd, emptyList(), emptyList(), emptyList(), emptyList())

        assertTrue(snapshot.gaps.isEmpty())
        assertNull(snapshot.longestGap)
        // Без Online-даних і записів весь неспаний проміжок рахується часом без телефону — коректний
        // деградований результат, не падіння.
        assertEquals(12 * 60, snapshot.phoneFreeMinutes)
    }

    @Test
    fun named_gap_is_attributed_to_its_category_and_summed() {
        val dayStart = at(14, 8)
        val dayEnd = at(14, 20)
        val gapA = GapCandidate(startTime = at(14, 9), durationMinutes = 45) // названо "reading"
        val gapB = GapCandidate(startTime = at(14, 11), durationMinutes = 30) // не назвали
        val gapC = GapCandidate(startTime = at(14, 14), durationMinutes = 60) // теж "reading"
        val entries = listOf(
            ActivityEntryEntity(
                categoryId = "reading", startTime = at(14, 9, 5), durationMinutes = 45,
                source = EntrySource.GAP_LABELED
            ),
            ActivityEntryEntity(
                categoryId = "reading", startTime = at(14, 14, 10), durationMinutes = 60,
                source = EntrySource.GAP_LABELED
            )
        )

        val snapshot = buildDaySnapshot(dayStart, dayEnd, listOf(gapA, gapB, gapC), entries, emptyList(), emptyList())

        assertEquals("reading", snapshot.gaps[0].categoryId)
        assertNull(snapshot.gaps[1].categoryId)
        assertEquals("reading", snapshot.gaps[2].categoryId)
        assertEquals(mapOf("reading" to 105), snapshot.namedMinutesByCategory)
        assertEquals(gapC.startTime, snapshot.longestGap?.startMillis) // 60 хв — найдовша
    }

    @Test
    fun online_and_entries_reduce_phone_free_minutes_without_double_counting_overlap() {
        val dayStart = at(14, 8)
        val dayEnd = at(14, 20) // 12 год
        // Online 09:00-10:00 накладається з ручним записом 09:30-10:30 (30 хв перекриття).
        val online = listOf(TimeSpan(at(14, 9), at(14, 10)))
        val entries = listOf(ActivityEntryEntity(categoryId = "reading", startTime = at(14, 9, 30), durationMinutes = 60))

        val snapshot = buildDaySnapshot(dayStart, dayEnd, emptyList(), entries, online, emptyList())

        // Зайнято разом лише 09:00-10:30 = 90 хв (не 60+60=120), тож вільно 12*60-90 = 630 хв.
        assertEquals(12 * 60 - 90, snapshot.phoneFreeMinutes)
    }
}
