package com.serkodesign.tepera.ui

import com.serkodesign.tepera.ui.calendar.MONTH_STICKER_AREA_HEIGHT
import com.serkodesign.tepera.ui.calendar.StickerShape
import com.serkodesign.tepera.ui.calendar.formatDurationClock
import com.serkodesign.tepera.ui.calendar.glyphFit
import com.serkodesign.tepera.ui.calendar.monthGrid
import com.serkodesign.tepera.ui.calendar.monthStickerSlots
import com.serkodesign.tepera.ui.calendar.stickerShapeFor
import com.serkodesign.tepera.ui.calendar.stickerTiltFor
import com.serkodesign.tepera.ui.calendar.weekDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

class CalendarModelTest {

    @Test
    fun monthGrid_consistsOfFullWeeksStartingOnFirstDay() {
        // Жовтень 2026 починається з четверга, 31 день → 5 тижнів при понеділку першим.
        val grid = monthGrid(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(35, grid.size)
        assertEquals(LocalDate.of(2026, 9, 28), grid.first())
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 1), grid.last())
    }

    @Test
    fun monthGrid_needsSixWeeksWhenMonthSpillsOver() {
        // Серпень 2026 починається з суботи → 6 рядків при понеділку першим.
        assertEquals(42, monthGrid(YearMonth.of(2026, 8), DayOfWeek.MONDAY).size)
    }

    @Test
    fun monthGrid_respectsSundayAsFirstDay() {
        val grid = monthGrid(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(DayOfWeek.SUNDAY, grid.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 27), grid.first())
    }

    @Test
    fun weekDays_containsGivenDateAndHasSevenDays() {
        val days = weekDays(LocalDate.of(2026, 10, 27), DayOfWeek.MONDAY)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 10, 26), days.first())
        assertTrue(LocalDate.of(2026, 10, 27) in days)
    }

    @Test
    fun stickerAppearance_isStableAndTiltIsBounded() {
        val ids = List(200) { "entry-$it" }
        ids.forEach { id ->
            assertEquals(stickerShapeFor(id), stickerShapeFor(id))
            assertTrue(stickerTiltFor(id) in -6f..6f)
        }
        // Різні записи не повинні всі мати одну форму.
        assertTrue(ids.map { stickerShapeFor(it) }.toSet().size > 1)
    }

    @Test
    fun monthStickerSlots_stayInsideCellForAnyCount() {
        for (count in 1..9) {
            val l = monthStickerSlots(count, 44f)
            val used = if (l.more) l.shown + 1 else l.shown
            assertTrue("count=$count", l.slots.size >= used)
            l.slots.take(used).forEach { (x, y) ->
                assertTrue("x count=$count", x >= 0f && x + l.size <= 44f + 0.01f)
                assertTrue("y count=$count", y >= 0f && y + l.size <= MONTH_STICKER_AREA_HEIGHT + 0.01f)
            }
        }
    }

    @Test
    fun monthStickerSlots_pileShowsFiveAndSixWithoutMoreChip() {
        assertEquals(5, monthStickerSlots(5, 44f).shown)
        assertEquals(6, monthStickerSlots(6, 44f).shown)
        assertTrue(!monthStickerSlots(6, 44f).more)
        val seven = monthStickerSlots(7, 44f)
        assertEquals(5, seven.shown)
        assertTrue(seven.more)
    }

    @Test
    fun glyphFit_heartGetsSmallerGlyphThanFullShapes() {
        assertTrue(glyphFit(StickerShape.HEART).scale < glyphFit(StickerShape.CIRCLE).scale)
    }

    @Test
    fun stickerShape_isSixKindsAndDependsOnlyOnId() {
        assertEquals(6, StickerShape.values().size)
        val ids = List(300) { "id-$it" }
        assertEquals(6, ids.map { stickerShapeFor(it) }.toSet().size)
        ids.forEach { assertEquals(stickerShapeFor(it), stickerShapeFor(it)) }
    }

    @Test
    fun formatDurationClock_usesHoursAndTwoDigitMinutes() {
        assertEquals("0:15", formatDurationClock(15))
        assertEquals("1:15", formatDurationClock(75))
        assertEquals("2:00", formatDurationClock(120))
        assertEquals("4:30", formatDurationClock(270))
        assertEquals("0:00", formatDurationClock(0))
        assertEquals("10:39", formatDurationClock(639))
    }
}
