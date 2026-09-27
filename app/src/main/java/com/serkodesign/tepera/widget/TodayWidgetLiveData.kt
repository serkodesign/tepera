package com.serkodesign.tepera.widget

import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.GapDetectionConfig
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.data.repository.GapCandidate
import com.serkodesign.tepera.util.TimeSpan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Важка (queryEvents) частина [DaySnapshot] — та частина, що НЕ реагує на живі Flow-записи. */
class TodayInputs(
    val hasUsageAccess: Boolean,
    val dayStartMillis: Long,
    val gapCandidates: List<GapCandidate>,
    val onlineIntervals: List<TimeSpan>,
    val isInsideSleepWindow: Boolean,
    val sleepWindows: List<SleepWindowEntity>
)

/**
 * W-3: кеш важких даних для віджета "Сьогодні" — той самий принцип, що [WidgetLiveData] для
 * старої сітки доби (окремий об'єкт, не той самий, бо форма даних інша: тут не 30-хв слоти, а
 * пряме сканування пауз). "Сьогодні" НЕ викликає `PauseRepository.persist()` — це лише перегляд,
 * не позначення (те належить W-4/"Пульт"), тож зайвого запису в `detected_gaps` при кожному
 * періодичному оновленні (WidgetUpdateWorker, ~30 хв) немає.
 */
object TodayWidgetLiveData {
    private const val CACHE_MILLIS = 5 * 60_000L
    private val computeLock = Mutex()
    private var cached: TodayInputs? = null
    private var lastGood: TodayInputs? = null
    private var cachedAt = 0L

    fun forceRefresh() { cached = null }

    suspend fun todayInputs(app: TeperaApp, nowMillis: Long): TodayInputs = computeLock.withLock {
        cached?.let { c -> if (nowMillis - cachedAt < CACHE_MILLIS) return@withLock c }
        try {
            compute(app, nowMillis).also { cached = it; lastGood = it; cachedAt = nowMillis }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Той самий запобіжник, що WidgetLiveData.gridInputs(): збій системного сервісу не
            // перетворює віджет на екран помилки, а показує останні відомі дані.
            lastGood ?: TodayInputs(true, nowMillis, emptyList(), emptyList(), false, emptyList())
        }
    }

    private suspend fun compute(app: TeperaApp, nowMillis: Long): TodayInputs {
        val hasAccess = app.balanceRepository.hasUsageAccess()
        if (!hasAccess) return TodayInputs(false, nowMillis, emptyList(), emptyList(), false, emptyList())
        val sleepWindows = app.sleepWindowRepository.getEnabledWindows()
        val dayStart = app.balanceRepository.calculateDayStartMillis(sleepWindows, searchEndMillis = nowMillis)
        val config = GapDetectionConfig.forSensitivity(app.settingsStore.gapSensitivity.first())
        val gaps = app.pauseRepository.scan(dayStart, nowMillis, sleepWindows, config).gaps
        val onlineIntervals = app.balanceRepository.getOnlineIntervals(dayStart, nowMillis)
        val isInsideSleepWindow = com.serkodesign.tepera.util.SleepWindowCalculator.isInsideWindow(sleepWindows, nowMillis)
        return TodayInputs(true, dayStart, gaps, onlineIntervals, isInsideSleepWindow, sleepWindows)
    }
}
