package com.serkodesign.tepera.widget

/** Один день тижневого патерну (W-1) — [hourlyPhoneFreeFraction] 24 значення `0f..1f`. */
data class DayHourlyPhoneFree(val dayStartMillis: Long, val hourlyPhoneFreeFraction: List<Float>)

/** Тиждень для віджета "Тиждень" (W-1) — [days] у хронологічному порядку, найдавніший перший. */
data class WeekSnapshot(val days: List<DayHourlyPhoneFree>)

/**
 * Чиста функція. [onlineMinutesByDay] — по одному 24-елементному масиву Online-хвилин НА ДЕНЬ
 * (`BalanceRepository.getOnlineMinutesPerSlot(midnight, 60, 24)`, викликаний ОКРЕМО для кожного
 * дня) — навмисно не `PatternRepository.hourlyOnlineMinutes()`, яка підсумовує весь запитаний
 * діапазон в ОДИН 24-масив, а не по днях (D-33: `WeekSnapshot` рахує лише з живої історії
 * `UsageStatsManager`, без фонових знімків CC-4, які власник скасував).
 *
 * Частка часу без телефону за годину = `1 - Online-хвилини / 60`, у межах `[0,1]`: година, що ще
 * не настала (сьогодні), чи передує `dayStart` (до пробудження), теж виходить `1.0` — Online там
 * `0` за визначенням. Окремого стану "немає даних" тут нема: поріг "менше 3 днів історії" (W-5)
 * перевіряється ДО виклику цієї функції, не в ній.
 */
fun buildWeekSnapshot(midnightsByDay: List<Long>, onlineMinutesByDay: List<IntArray>): WeekSnapshot {
    require(midnightsByDay.size == onlineMinutesByDay.size) {
        "midnightsByDay (${midnightsByDay.size}) і onlineMinutesByDay (${onlineMinutesByDay.size}) мають бути однакової довжини"
    }
    val days = midnightsByDay.indices.map { index ->
        val minutes = onlineMinutesByDay[index]
        val fractions = (0 until 24).map { hour ->
            val online = minutes.getOrElse(hour) { 0 }
            (1f - online / 60f).coerceIn(0f, 1f)
        }
        DayHourlyPhoneFree(midnightsByDay[index], fractions)
    }
    return WeekSnapshot(days)
}
