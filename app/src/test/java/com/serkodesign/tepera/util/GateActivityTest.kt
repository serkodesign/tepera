package com.serkodesign.tepera.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class GateActivityTest {

    private val zone = ZoneId.of("Europe/Kyiv")

    // 2026-09-21 — понеділок
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 9, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun schedule(vararg entries: Pair<DayOfWeek, List<TimeInterval>>) = GateSchedule(mapOf(*entries))

    @Test
    fun noSchedule_meansAlways_unlessPaused() {
        assertTrue(GateActivity.isActive(at(21, 12), null, emptyList(), zone))
        val pause = PauseWindow(at(21, 8), at(22, 0))
        assertFalse(GateActivity.isActive(at(21, 12), null, listOf(pause), zone))
        assertTrue(GateActivity.isActive(at(22, 0), null, listOf(pause), zone)) // пауза скінчилась
    }

    @Test
    fun simpleInterval_isHalfOpen() {
        val s = schedule(DayOfWeek.MONDAY to listOf(TimeInterval(9 * 60, 18 * 60)))
        assertFalse(s.isActiveAt(at(21, 8, 59), zone))
        assertTrue(s.isActiveAt(at(21, 9), zone))
        assertTrue(s.isActiveAt(at(21, 17, 59), zone))
        assertFalse(s.isActiveAt(at(21, 18), zone))
    }

    @Test
    fun twoIntervalsPerDay() {
        val s = schedule(DayOfWeek.MONDAY to listOf(TimeInterval(8 * 60, 10 * 60), TimeInterval(20 * 60, 22 * 60)))
        assertTrue(s.isActiveAt(at(21, 9), zone))
        assertFalse(s.isActiveAt(at(21, 12), zone))
        assertTrue(s.isActiveAt(at(21, 21), zone))
    }

    @Test
    fun intervalCrossingMidnight_continuesIntoNextDay() {
        val s = schedule(DayOfWeek.MONDAY to listOf(TimeInterval(22 * 60, 6 * 60)))
        assertFalse(s.isActiveAt(at(21, 21), zone))
        assertTrue(s.isActiveAt(at(21, 23), zone))
        assertTrue(s.isActiveAt(at(22, 5, 59), zone)) // вівторок вранці — хвіст понеділка
        assertFalse(s.isActiveAt(at(22, 6), zone))
        assertFalse(s.isActiveAt(at(28, 5), zone)) // наступний понеділок вранці: хвіст неділі, а неділя порожня
    }

    @Test
    fun dayWithoutIntervals_isInactive_andEqualTimesMeanWholeDay() {
        val s = schedule(DayOfWeek.TUESDAY to listOf(TimeInterval(0, 0)))
        assertFalse(s.isActiveAt(at(21, 12), zone)) // понеділок без проміжків
        assertTrue(s.isActiveAt(at(22, 3), zone))
        assertTrue(s.isActiveAt(at(22, 23, 59), zone))
    }

    @Test
    fun pauseWinsOverSchedule() {
        val s = schedule(DayOfWeek.MONDAY to listOf(TimeInterval(0, 0)))
        assertFalse(GateActivity.isActive(at(21, 12), s, listOf(PauseWindow(at(21, 0), at(22, 0))), zone))
    }

    @Test
    fun encodeDecode_roundTrips_andGarbageMeansAlways() {
        val s = schedule(
            DayOfWeek.MONDAY to listOf(TimeInterval(540, 1080), TimeInterval(1200, 60)),
            DayOfWeek.SUNDAY to emptyList()
        )
        val back = GateSchedule.decode(s.encode())!!
        assertEquals(s.intervalsOf(DayOfWeek.MONDAY), back.intervalsOf(DayOfWeek.MONDAY))
        assertEquals(emptyList<TimeInterval>(), back.intervalsOf(DayOfWeek.SUNDAY))
        assertNull(GateSchedule.decode("нісенітниця"))
        assertNull(GateSchedule.decode(null))
    }

    @Test
    fun wholeWeek_isActiveAlways() {
        assertTrue(GateSchedule.wholeWeek().isActiveAt(at(23, 3), zone))
    }

    @Test
    fun presets_today_endsAtNextMidnight() {
        val p = GatePausePresets.today(at(21, 15), zone)
        assertEquals(at(22, 0), p.untilMillis)
        assertEquals(at(21, 15), p.fromMillis)
    }

    @Test
    fun presets_weekend_fromWeekday_startsSaturday_endsMonday() {
        val p = GatePausePresets.weekend(at(23, 12), zone) // середа
        assertEquals(at(26, 0), p.fromMillis) // субота 26 вер.
        assertEquals(at(28, 0), p.untilMillis) // понеділок 28 вер.
    }

    @Test
    fun presets_weekend_onSunday_startsNowEndsMonday() {
        val now = at(27, 20)
        val p = GatePausePresets.weekend(now, zone)
        assertEquals(now, p.fromMillis)
        assertEquals(at(28, 0), p.untilMillis)
    }

    @Test
    fun presets_untilDate_includesThatWholeDay() {
        val p = GatePausePresets.untilDate(at(21, 10), LocalDate.of(2026, 9, 25), zone)
        assertEquals(at(26, 0), p.untilMillis)
    }
}
