package com.serkodesign.tepera.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * CC-8: коли надсилати єдине тижневе сповіщення — наступної неділі о [HOUR]:00 за локальним часом
 * (строго після [nowMillis]). Чиста арифметика, тестується без пристрою.
 */
object WeeklySummarySchedule {
    const val HOUR = 19

    fun nextRunMillis(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var date = now.toLocalDate()
        var candidate = date.atTime(LocalTime.of(HOUR, 0)).atZone(zone)
        while (candidate.dayOfWeek != DayOfWeek.SUNDAY || !candidate.toInstant().isAfter(now.toInstant())) {
            date = date.plusDays(1)
            candidate = date.atTime(LocalTime.of(HOUR, 0)).atZone(zone)
        }
        return candidate.toInstant().toEpochMilli()
    }
}
