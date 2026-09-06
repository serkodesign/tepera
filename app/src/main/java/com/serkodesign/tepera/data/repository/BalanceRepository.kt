package com.serkodesign.tepera.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Результат розрахунку балансу на конкретний момент. */
data class OnlineOfflineBalance(
    val onlineMinutesToday: Int,
    val offlineMinutesToday: Int, // сума ActivityEntryEntity за сьогодні — рахує ActivityRepository окремо
    val targetMinutes: Int,
    val denominatorMinutes: Int, // FR-3.2: max(180, хвилин_від_00:00)
    val onlineRatio: Float // onlineMinutesToday / denominatorMinutes, для UI-шкали
)

/**
 * FR-3.1–3.6: доступ до статистики використання (UsageStatsManager) + Grace Period Buffer.
 *
 * ВАЖЛИВО: PACKAGE_USAGE_STATS — protected permission. queryUsageStats() поверне ПОРОЖНІЙ
 * список, якщо доступ не наданий через Налаштування (не кине exception) — саме тому
 * hasUsageAccess() перевіряється окремо, це і є основа для fallback-стану (FR-3.6).
 */
class BalanceRepository(
    private val context: Context,
    private val excludedAppDao: ExcludedAppDao,
    private val defaultTargetMinutes: Int = 180 // FR-3.4 дефолт, користувач може змінити через DataStore
) {

    /**
     * Перевірка, чи наданий доступ. Стандартний спосіб — спробувати запит і подивитись,
     * чи є хоч якийсь результат за коротким контрольним інтервалом; альтернатива —
     * AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS). Обрати підхід на етапі реалізації.
     */
    suspend fun hasUsageAccess(): Boolean = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 1000 * 60 * 60 // остання година як контрольний інтервал
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        stats.isNotEmpty()
    }

    /**
     * FR-3.1, FR-3.5: сумарний Online-час за сьогодні, за вирахуванням застосунків
     * зі списку виключень.
     */
    suspend fun getOnlineMinutesToday(): Int = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startOfDay = startOfTodayMillis()
        val now = System.currentTimeMillis()

        val excluded = excludedAppDao.getExcludedPackageNames().toSet()

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
        val totalForegroundMs = stats
            .filterNot { it.packageName in excluded }
            .sumOf { it.totalTimeInForeground }

        (totalForegroundMs / 60_000L).toInt()
    }

    /** FR-3.2: Grace Period Buffer — знаменник ніколи не менший за 180 хв. */
    fun calculateDenominatorMinutes(): Int {
        val minutesSinceMidnight = minutesSinceStartOfDay()
        return maxOf(180, minutesSinceMidnight)
    }

    /** FR-3.3: локальна північ (узгоджується з відомим timezone-обмеженням, SRS розділ 11). */
    private fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun minutesSinceStartOfDay(): Int =
        ((System.currentTimeMillis() - startOfTodayMillis()) / 60_000L).toInt()
}
