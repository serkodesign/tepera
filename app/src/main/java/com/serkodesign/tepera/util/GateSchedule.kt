package com.serkodesign.tepera.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Проміжок дня в хвилинах від півночі (локальний час). [startMinute] == [endMinute] — ВЕСЬ день;
 * [endMinute] < [startMinute] — проміжок переходить через північ і закінчується наступного дня
 * (належить дню, у якому починається).
 */
data class TimeInterval(val startMinute: Int, val endMinute: Int) {
    val isWholeDay: Boolean get() = startMinute == endMinute
    val crossesMidnight: Boolean get() = endMinute < startMinute

    /** Чи діє проміжок у хвилину [minute] ТОГО САМОГО дня, у якому він починається. */
    fun coversOnStartDay(minute: Int): Boolean = when {
        isWholeDay -> true
        crossesMidnight -> minute >= startMinute
        else -> minute in startMinute until endMinute
    }

    /** Чи діє "хвіст" проміжку, що почався ВЧОРА й перейшов через північ, у хвилину [minute] сьогодні. */
    fun coversNextDayTail(minute: Int): Boolean = crossesMidnight && minute < endMinute
}

/**
 * CC-5: розклад воріт — спільний для всіх воріт; дні тижня, до [MAX_INTERVALS_PER_DAY] проміжків на
 * день; день без проміжків — ворота цього дня не діють. Відсутній розклад (`null` у сховищі)
 * означає "завжди" — поведінка за замовчуванням не змінюється. Час локальний.
 */
data class GateSchedule(val days: Map<DayOfWeek, List<TimeInterval>>) {

    fun intervalsOf(day: DayOfWeek): List<TimeInterval> = days[day].orEmpty()

    fun isActiveAt(moment: ZonedDateTime): Boolean {
        val minute = moment.hour * 60 + moment.minute
        val today = moment.dayOfWeek
        if (intervalsOf(today).any { it.coversOnStartDay(minute) }) return true
        return intervalsOf(today.minus(1)).any { it.coversNextDayTail(minute) }
    }

    fun isActiveAt(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        isActiveAt(Instant.ofEpochMilli(millis).atZone(zone))

    /** Формат: `1=540-1080,1200-60;2=;...` (ISO-номер дня, потім проміжки). */
    fun encode(): String = DayOfWeek.entries.joinToString(";") { day ->
        "${day.value}=" + intervalsOf(day).joinToString(",") { "${it.startMinute}-${it.endMinute}" }
    }

    companion object {
        const val MAX_INTERVALS_PER_DAY = 2

        /** Усі дні з одним проміжком "весь день" — еквівалент "завжди", початкова точка для редагування. */
        fun wholeWeek(): GateSchedule =
            GateSchedule(DayOfWeek.entries.associateWith { listOf(TimeInterval(0, 0)) })

        /** @return `null` для порожнього/пошкодженого рядка (тоді діє "завжди"). */
        fun decode(text: String?): GateSchedule? {
            if (text.isNullOrBlank()) return null
            return runCatching {
                val map = text.split(";").associate { part ->
                    val (dayText, intervalsText) = part.split("=", limit = 2)
                    val day = DayOfWeek.of(dayText.trim().toInt())
                    val intervals = intervalsText.split(",").filter { it.isNotBlank() }.map { item ->
                        val (start, end) = item.split("-")
                        TimeInterval(start.trim().toInt().coerceIn(0, 1439), end.trim().toInt().coerceIn(0, 1439))
                    }.take(MAX_INTERVALS_PER_DAY)
                    day to intervals
                }
                GateSchedule(map)
            }.getOrNull()
        }
    }
}

/** Пауза воріт: активна в [fromMillis, untilMillis). [fromMillis] може бути в майбутньому ("на вихідні" серед тижня). */
data class PauseWindow(val fromMillis: Long, val untilMillis: Long) {
    fun contains(millis: Long): Boolean = millis in fromMillis until untilMillis
}

/** Прості пресети паузи; усі рахуються за локальним часом. */
object GatePausePresets {

    /** Від зараз до наступної локальної півночі. */
    fun today(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): PauseWindow {
        val tomorrow = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().plusDays(1)
        return PauseWindow(nowMillis, tomorrow.atStartOfDay(zone).toInstant().toEpochMilli())
    }

    /**
     * Найближчі вихідні (субота-неділя): у суботу чи неділю — від зараз до понеділка 00:00; в інші дні —
     * від суботи 00:00 до понеділка 00:00 (ворота в будні лишаються активними).
     */
    fun weekend(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): PauseWindow {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val isWeekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
        val daysToSaturday = (DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7
        val daysToMonday = (DayOfWeek.MONDAY.value - today.dayOfWeek.value + 7) % 7
        val saturday = today.plusDays(daysToSaturday.toLong())
        val monday = today.plusDays((if (daysToMonday == 0) 7 else daysToMonday).toLong())
        val from = if (isWeekend) nowMillis else saturday.atStartOfDay(zone).toInstant().toEpochMilli()
        return PauseWindow(from, monday.atStartOfDay(zone).toInstant().toEpochMilli())
    }

    /** Від зараз до кінця вибраної дати включно (до початку наступного дня). */
    fun untilDate(nowMillis: Long, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): PauseWindow =
        PauseWindow(nowMillis, date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
}

/**
 * CC-5: єдине правило — **ворота активні = зараз вікно розкладу І немає паузи**. Інакше ярлик одразу
 * відкриває цільовий застосунок. [schedule] == `null` — "завжди".
 */
object GateActivity {
    fun isActive(
        atMillis: Long,
        schedule: GateSchedule?,
        pauses: List<PauseWindow>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (pauses.any { it.contains(atMillis) }) return false
        return schedule?.isActiveAt(atMillis, zone) ?: true
    }
}
