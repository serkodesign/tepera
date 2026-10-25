package com.serkodesign.tepera.domain.risk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SlotSplitterTest {
    private val kyiv = ZoneId.of("Europe/Kyiv")
    private fun millis(dt: LocalDateTime) = dt.atZone(kyiv).toInstant().toEpochMilli()

    private fun collect(start: Long, end: Long): Map<Pair<Long, Int>, Long> {
        val out = HashMap<Pair<Long, Int>, Long>()
        SlotSplitter.split(start, end, kyiv) { day, slot, ms -> out.merge(day to slot, ms, Long::plus) }
        return out
    }

    @Test
    fun `session splits on quarter hour boundaries`() {
        val day = LocalDate.of(2026, 10, 20)
        val r = collect(millis(day.atTime(22, 10)), millis(day.atTime(22, 40)))
        assertEquals(5 * 60_000L, r[day.toEpochDay() to 88])
        assertEquals(15 * 60_000L, r[day.toEpochDay() to 89])
        assertEquals(10 * 60_000L, r[day.toEpochDay() to 90])
    }

    @Test
    fun `fall back day of 25 hours keeps total time and doubles repeated hour`() {
        // 25.10.2026 о 04:00 літній час -> 03:00 зимовий: година 03:xx повторюється.
        val day = LocalDate.of(2026, 10, 25)
        val start = millis(day.atTime(0, 0))
        val end = millis(day.plusDays(1).atTime(0, 0))
        assertEquals(25 * 3_600_000L, end - start)
        val r = collect(start, end)
        assertEquals(25 * 3_600_000L, r.values.sum())
        // Слот 03:00 набрав 15 хв двічі.
        assertEquals(30 * 60_000L, r[day.toEpochDay() to 12])
        assertEquals(15 * 60_000L, r[day.toEpochDay() to 8])
    }

    @Test
    fun `spring forward day of 23 hours has no slots in the skipped hour`() {
        // 29.03.2026 о 03:00 -> 04:00: година 03:xx пропущена.
        val day = LocalDate.of(2026, 3, 29)
        val start = millis(day.atTime(0, 0))
        val end = millis(day.plusDays(1).atTime(0, 0))
        assertEquals(23 * 3_600_000L, end - start)
        val r = collect(start, end)
        assertEquals(23 * 3_600_000L, r.values.sum())
        for (slot in 12..15) assertEquals(null, r[day.toEpochDay() to slot])
    }
}
