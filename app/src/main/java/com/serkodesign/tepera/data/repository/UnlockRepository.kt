package com.serkodesign.tepera.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.util.SleepWindowCalculator
import com.serkodesign.tepera.util.localStartOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * T-14 (tepera-dev-spec.md): лічильник розблокувань — `UsageEvents.Event.KEYGUARD_HIDDEN`,
 * доступний лише з API 28 (Android 9). Документ прямо забороняє апроксимацію нижче 28: "людина
 * порівнюватиме число з відчуттям і не довірятиме застосунку" — тому [isSupported] на старіших
 * версіях повертає `false`, і викликач ховає фічу цілком, а не показує щось приблизне.
 * Нижня межа мала б підтвердитись спайком T-1 (пункт 5) — спайк ще не виконаний, тож тут
 * застосовано буквальний фолбек документа "якщо надійного джерела немає, фіча приховується".
 */
class UnlockRepository(private val context: Context) {

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /**
     * Кількість KEYGUARD_HIDDEN у [from, to), за вирахуванням тих, що потрапляють у ввімкнене
     * вікно сну (T-12) — "розблокування всередині вікна сну не рахуються". Порожній список вікон
     * за замовчуванням — без фільтрації.
     */
    suspend fun countUnlocks(from: Long, to: Long, sleepWindows: List<SleepWindowEntity> = emptyList()): Int =
        withContext(Dispatchers.IO) {
            if (!isSupported()) return@withContext 0
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            try {
                val events = usm.queryEvents(from, to)
                val event = UsageEvents.Event()
                var count = 0
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN &&
                        !SleepWindowCalculator.isInsideWindow(sleepWindows, event.timeStamp)
                    ) {
                        count++
                    }
                }
                count
            } catch (e: SecurityException) {
                0
            }
        }

    /**
     * Розблокування по календарних добах за один прохід по подіях [from, to): ключ — початок доби.
     * Системна історія подій сягає лише ~7-9 діб, тож доби ДО найдавнішої відомої події в мапі
     * відсутні (а не "0") — нуль там був би хибним фактом, а не відсутністю даних (документ T-14
     * забороняє показувати щось неточне). Доба, у якій є найдавніша подія, зараховується.
     */
    suspend fun countUnlocksByDay(
        from: Long,
        to: Long,
        sleepWindows: List<SleepWindowEntity> = emptyList()
    ): Map<Long, Int> = withContext(Dispatchers.IO) {
        if (!isSupported()) return@withContext emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        try {
            val events = usm.queryEvents(from, to)
            val event = UsageEvents.Event()
            val counts = HashMap<Long, Int>()
            var earliest: Long? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (earliest == null || event.timeStamp < earliest) earliest = event.timeStamp
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN &&
                    !SleepWindowCalculator.isInsideWindow(sleepWindows, event.timeStamp)
                ) {
                    val day = localStartOfDay(event.timeStamp)
                    counts[day] = (counts[day] ?: 0) + 1
                }
            }
            val firstDay = earliest?.let { localStartOfDay(it) } ?: return@withContext emptyMap()
            // Кожна доба від найдавнішої відомої до сьогодні — без записів у counts це справжній 0.
            val result = HashMap<Long, Int>()
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = firstDay }
            while (cal.timeInMillis < to) {
                result[cal.timeInMillis] = counts[cal.timeInMillis] ?: 0
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            result
        } catch (e: SecurityException) {
            emptyMap()
        }
    }
}
