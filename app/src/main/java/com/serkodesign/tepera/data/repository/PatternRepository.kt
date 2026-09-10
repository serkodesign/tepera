package com.serkodesign.tepera.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import com.serkodesign.tepera.util.systemExclusionPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

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
            val events = usm.queryEvents(from, to)
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
            for (start in foregroundSince.values) addToHourBuckets(buckets, start, to)
        } catch (e: SecurityException) {
            // buckets лишаються нульовими — PatternViewModel трактує це як "нема доступу" вище по стеку.
        }
        buckets.toList()
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
