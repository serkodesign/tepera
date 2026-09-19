package com.serkodesign.tepera.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import com.serkodesign.tepera.util.startOfTodayMillis
import com.serkodesign.tepera.util.systemExclusionPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/**
 * FR-D.8 (SRS v2.6): "тепловий" патерн доби — Online-хвилини по кожній годині доби (0–23),
 * підсумовані за кілька календарних днів. Та сама Online-семантика, що BalanceRepository
 * (Exclusion List + виключення лаунчера/клавіатури через спільний systemExclusionPackages()) —
 * це патерн ТОГО САМОГО "Online", яке показує структура доби, не сирого дотику до екрана
 * (на відміну від PauseRepository, де пауза — про сам факт дотику, FR-D.2).
 */
class PatternRepository(
    private val context: Context,
    private val excludedAppDao: ExcludedAppDao
) {

    /** Online-хвилини за кожну з 24 годин доби, сумарно за проміжок [from, to). */
    suspend fun hourlyOnlineMinutes(from: Long, to: Long): List<Int> = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val excluded = excludedAppDao.getExcludedPackageNames().toSet() + systemExclusionPackages(context)
        val buckets = IntArray(24)
        try {
            // Окремий запит на кожну добу: застосунок без завершальної події "у фон" лишається
            // в foregroundSince до кінця вікна, і для тижня/місяця це розтягувало одну загублену
            // подію на всі наступні дні, зафарбовуючи всі 24 години (перевірено на Samsung S23:
            // усі години тижня були 30-60 хв за тренду 1-10 год/добу).
            var dayStart = from
            while (dayStart < to) {
                val dayEnd = minOf(dayStart + DAY_MILLIS, to)
                val events = usm.queryEvents(dayStart, dayEnd)
                val event = UsageEvents.Event()
                val foregroundSince = HashMap<String, Long>()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val packageName = event.packageName ?: continue
                    if (packageName in excluded) continue
                    when (event.eventType) {
                        UsageEvents.Event.MOVE_TO_FOREGROUND -> foregroundSince[packageName] = event.timeStamp
                        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                            val start = foregroundSince.remove(packageName)
                            if (start != null) addToHourBuckets(buckets, start, event.timeStamp)
                        }
                    }
                }
                for (start in foregroundSince.values) addToHourBuckets(buckets, start, dayEnd)
                dayStart = dayEnd
            }
        } catch (e: SecurityException) {
            // buckets лишаються нульовими — PatternViewModel трактує це як "нема доступу" вище по стеку.
        }
        buckets.toList()
    }

    /**
     * T-2 (tepera-dev-spec.md): скільки ПОВНИХ днів реальної історії `UsageEvents` фактично
     * доступно перед [nowMillis], обмежено [maxDays] — НЕ "днів з моменту встановлення Tepera"
     * (`SettingsStore.firstLaunchMillis`, чинний до T-2 сигнал готовності патерну). Системна
     * історія використання існує незалежно від того, коли встановлено Tepera — щойно доступ до
     * статистики надано, її вже можна показати, не чекаючи штучний тиждень. Шукає
     * найдавнішу подію в [nowMillis - maxDays*DAY_MILLIS, nowMillis); 0, якщо подій нема взагалі
     * (свіжий пристрій/немає доступу).
     */
    suspend fun availableHistoryDays(nowMillis: Long, maxDays: Int): Int = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        try {
            // Рахуємо ПОВНІ календарні доби до початку сьогодні (саме таке вікно потім читає
            // hourlyOnlineMinutes) і будь-яка подія в добі зараховує її. Раніше було
            // floor((now - earliest) / доба) від "зараз": найдавніша подія майже завжди трохи
            // пізніше за now - maxDays, тож тиждень давав 6 днів, а місяць — 29.
            val todayStart = startOfTodayMillis()
            val from = todayStart - maxDays * DAY_MILLIS
            val events = usm.queryEvents(from, nowMillis)
            val event = UsageEvents.Event()
            var earliest: Long? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (earliest == null || event.timeStamp < earliest) earliest = event.timeStamp
            }
            val earliestFound = earliest ?: return@withContext 0
            val fullDays = (todayStart - earliestFound + DAY_MILLIS - 1) / DAY_MILLIS
            fullDays.toInt().coerceIn(0, maxDays)
        } catch (e: SecurityException) {
            0
        }
    }

    /** Ділить [startMillis, endMillis) по межах годин доби й додає хвилини у відповідні бакети. */
    private fun addToHourBuckets(buckets: IntArray, startMillis: Long, endMillis: Long) {
        var cursor = startMillis
        while (cursor < endMillis) {
            val cal = Calendar.getInstance().apply { timeInMillis = cursor }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.HOUR_OF_DAY, 1)
            val hourBoundary = cal.timeInMillis
            val segmentEnd = minOf(endMillis, hourBoundary)
            buckets[hour] += ((segmentEnd - cursor) / 60_000L).toInt()
            cursor = segmentEnd
        }
    }
}
