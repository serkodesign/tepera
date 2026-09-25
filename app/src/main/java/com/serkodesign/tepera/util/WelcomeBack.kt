package com.serkodesign.tepera.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * CC-4: чиста логіка «повернення без провини» (без Room/Context — тестується без пристрою).
 * Підсумок з'являється, коли з останнього відкриття Tepera минуло [MIN_GAP_DAYS] і більше діб;
 * він описує ці дні нейтрально (що відмічено, скільки було Online), без слів про «пропущене».
 */
object WelcomeBack {
    const val MIN_GAP_DAYS = 3
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Чи минуло від [lastOpenMillis] до [nowMillis] щонайменше [MIN_GAP_DAYS] діб. 0 = ще ніколи не відкривали. */
    fun gapReached(lastOpenMillis: Long, nowMillis: Long): Boolean =
        lastOpenMillis > 0 && nowMillis - lastOpenMillis >= MIN_GAP_DAYS * DAY_MILLIS

    /**
     * Повні календарні доби між днем останнього відкриття (не включно) і сьогоднішнім днем (не включно):
     * день останнього відкриття людина частково бачила сама, сьогодні ще триває.
     */
    fun periodDays(lastOpenMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<LocalDate> {
        val first = Instant.ofEpochMilli(lastOpenMillis).atZone(zone).toLocalDate().plusDays(1)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val days = mutableListOf<LocalDate>()
        var day = first
        while (day.isBefore(today)) {
            days += day
            day = day.plusDays(1)
        }
        return days
    }
}

/** Запис активності для підсумку: початок і тривалість у хвилинах. */
data class EntrySpan(val startMillis: Long, val minutes: Int)

/**
 * Підсумок днів, коли людина не відкривала застосунок. [loggedMinutes] — сума відмічених активностей;
 * [averageOnlineMinutes] — середнє Online на день за дні, для яких Online відомий (системна історія подій),
 * `null` якщо жодного такого дня.
 */
data class WelcomeBackSummary(val days: Int, val loggedMinutes: Int, val averageOnlineMinutes: Int?) {

    companion object {
        /** @return `null`, якщо показувати нема чого (менше двох повних діб або жодного числа). */
        fun build(
            days: List<LocalDate>,
            onlineByEpochDay: Map<Long, Int>,
            entries: List<EntrySpan>,
            zone: ZoneId = ZoneId.systemDefault()
        ): WelcomeBackSummary? {
            if (days.size < 2) return null
            val from = days.first().atStartOfDay(zone).toInstant().toEpochMilli()
            val to = days.last().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val logged = entries.filter { it.startMillis in from until to }.sumOf { it.minutes }
            val online = days.mapNotNull { onlineByEpochDay[it.toEpochDay()] }
            val average = if (online.isEmpty()) null else online.sum() / online.size
            if (logged == 0 && average == null) return null
            return WelcomeBackSummary(days.size, logged, average)
        }
    }
}

/** CC-4: Online можна показати лише за доби, які системна історія подій покриває повністю. */
object HistoryCoverage {

    /**
     * Доби з [days], що починаються не раніше найдавнішої події в системній історії ([earliestEventMillis]).
     * Доба, що починається до неї, покрита неповністю — нуль там був би хибним фактом, тож її не враховуємо.
     * `null` — історії нема (немає доступу до статистики): жодна доба не покрита.
     */
    fun coveredDays(days: List<LocalDate>, earliestEventMillis: Long?, zone: ZoneId = ZoneId.systemDefault()): List<LocalDate> {
        if (earliestEventMillis == null) return emptyList()
        return days.filter { it.atStartOfDay(zone).toInstant().toEpochMilli() >= earliestEventMillis }
    }
}
