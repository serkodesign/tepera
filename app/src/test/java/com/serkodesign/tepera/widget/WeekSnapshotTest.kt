package com.serkodesign.tepera.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** W-1: `buildWeekSnapshot()` — чиста функція, 7 днів × 24 години. */
class WeekSnapshotTest {

    @Test
    fun full_hour_online_gives_zero_phone_free_fraction() {
        val minutes = IntArray(24)
        minutes[10] = 60 // 10:00-11:00 повністю Online
        val week = buildWeekSnapshot(listOf(1_000L), listOf(minutes))

        assertEquals(0f, week.days.single().hourlyPhoneFreeFraction[10], 0.0001f)
    }

    @Test
    fun zero_online_minutes_gives_full_phone_free_fraction() {
        val week = buildWeekSnapshot(listOf(1_000L), listOf(IntArray(24)))

        week.days.single().hourlyPhoneFreeFraction.forEach { fraction ->
            assertEquals(1f, fraction, 0.0001f)
        }
    }

    @Test
    fun partial_online_minutes_give_a_fractional_value() {
        val minutes = IntArray(24)
        minutes[9] = 15 // 15 з 60 хв Online -> 45 хв (75%) без телефону
        val week = buildWeekSnapshot(listOf(1_000L), listOf(minutes))

        assertEquals(0.75f, week.days.single().hourlyPhoneFreeFraction[9], 0.0001f)
    }

    @Test
    fun fraction_is_clamped_when_online_minutes_exceed_sixty() {
        // Не мало б статись за нормальної роботи BalanceRepository, але сумарна функція не повинна
        // повертати від'ємну частку, якщо джерело раптом дало більше 60 хв на годину.
        val minutes = IntArray(24)
        minutes[5] = 90
        val week = buildWeekSnapshot(listOf(1_000L), listOf(minutes))

        assertEquals(0f, week.days.single().hourlyPhoneFreeFraction[5], 0.0001f)
    }

    @Test
    fun days_are_returned_in_the_given_chronological_order() {
        val week = buildWeekSnapshot(
            listOf(100L, 200L, 300L),
            listOf(IntArray(24), IntArray(24), IntArray(24))
        )

        assertEquals(listOf(100L, 200L, 300L), week.days.map { it.dayStartMillis })
    }

    @Test
    fun mismatched_list_sizes_are_rejected() {
        assertThrows(IllegalArgumentException::class.java) {
            buildWeekSnapshot(listOf(100L, 200L), listOf(IntArray(24)))
        }
    }

    @Test
    fun a_week_spanning_a_dst_transition_still_produces_seven_ordered_days() {
        // Реальний прохід через SnapshotTimeTest.seven_day_sequence_spanning_dst — тут лише
        // підтверджуємо, що WeekSnapshot будується з такої послідовності без падіння чи перекосу.
        val midnights = (6 downTo 0).map { daysAgo -> addDaysMillis(1_700_000_000_000L, -daysAgo) }
        val week = buildWeekSnapshot(midnights, midnights.map { IntArray(24) })

        assertEquals(7, week.days.size)
        assertEquals(midnights, week.days.map { it.dayStartMillis })
    }
}
