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
 * ВАЖЛИВО: PACKAGE_USAGE_STATS — protected permission. Коли доступ НІКОЛИ не надавався,
 * queryUsageStats() справді повертає порожній список, без винятку. Але якщо доступ був
 * наданий, а тоді користувач вручну вимкнув його в Налаштуваннях (Спеціальний доступ) —
 * queryUsageStats() кидає SecurityException, а не повертає порожній список (підтверджено
 * крашем на реальному пристрої). Обидва методи нижче явно ловлять SecurityException, інакше
 * відкликаний посеред сесії доступ ламає fallback з FR-3.6 замість вмикати його.
 */
class BalanceRepository(
    private val context: Context,
    private val excludedAppDao: ExcludedAppDao
    // FR-3.4: сам таргет (дефолт 180 хв, налаштовується) живе в SettingsStore, не тут —
    // BalanceRepository лише рахує Online-хвилини й знаменник Grace Period Buffer.
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
        try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            stats.isNotEmpty()
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * FR-3.1, FR-3.5: сумарний Online-час за сьогодні, за вирахуванням застосунків
     * зі списку виключень.
     */
    suspend fun getOnlineMinutesToday(): Int = getOnlineMinutes(startOfTodayMillis(), System.currentTimeMillis())

    /**
     * FR-5.3: узагальнена версія getOnlineMinutesToday() для довільного інтервалу — потрібна
     * для тижневого тренду балансу (по одному запиту на кожен з минулих днів, той самий підхід,
     * що й для "сьогодні", а не один запит з INTERVAL_DAILY на весь тиждень: бакетизація
     * queryUsageStats() по межах діб не гарантовано збігається з локальною північчю).
     */
    suspend fun getOnlineMinutes(from: Long, to: Long): Int = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val excluded = excludedAppDao.getExcludedPackageNames().toSet()

        try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, from, to)
            val totalForegroundMs = stats
                .filterNot { it.packageName in excluded }
                .sumOf { it.totalTimeInForeground }

            (totalForegroundMs / 60_000L).toInt()
        } catch (e: SecurityException) {
            0
        }
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
