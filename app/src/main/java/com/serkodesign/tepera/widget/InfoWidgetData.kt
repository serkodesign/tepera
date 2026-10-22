package com.serkodesign.tepera.widget

import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.util.TimeSpan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Важка (сканування UsageEvents) частина даних інформаційних віджетів "Widgets v2": Online від точки старту дня
 * до "зараз" і межі доби. Ті самі виклики, що `BalanceViewModel.refresh()` на Home, — тож числа на віджеті
 * збігаються з Home. Записи й таймери віджети читають живими потоками (див. [InfoWidget]), тут їх нема.
 */
class OnlineFacts(
    val hasUsageAccess: Boolean,
    val dayStartMillis: Long,
    val windows: List<SleepWindowEntity>,
    val onlineMinutes: Int,
    val intervals: List<TimeSpan>,
    val dayLengthMinutes: Int,
    val daySpanMinutes: Int,
    val targetMinutes: Int?
) {
    /** Хвилини від точки старту дня до "зараз"; 0, поки день ще не почався. */
    val elapsedMinutes: Int get() = ((System.currentTimeMillis() - dayStartMillis) / 60_000L).toInt().coerceAtLeast(0)

    /** Точка старту дня — "зараз", поки не було першого суттєвого розблокування (BalanceRepository). */
    val dayStarted: Boolean get() = elapsedMinutes >= 1
}

object InfoWidgetData {
    private const val CACHE_MILLIS = 5 * 60_000L

    // Після WidgetLiveData.forceRefresh() усі екземпляри віджетів просять дані одночасно — розрахунок один на всіх
    // (той самий принцип і причина, що в WidgetLiveData.gridInputs).
    private val lock = Mutex()
    private var cached: OnlineFacts? = null
    private var cachedAt = 0L
    private var cachedTick = -1L

    suspend fun facts(app: TeperaApp): OnlineFacts = lock.withLock {
        val now = System.currentTimeMillis()
        val tick = WidgetLiveData.refreshTick.value
        cached?.let { if (cachedTick == tick && now - cachedAt < CACHE_MILLIS) return@withLock it }
        try {
            compute(app, now).also {
                cached = it
                cachedAt = now
                cachedTick = tick
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Збій системного сервісу статистики не повинен ламати віджет: віддаємо останнє відоме (навіть застаріле)
            // або "без даних"; збій не кешуємо — наступне оновлення спробує ще раз.
            cached ?: OnlineFacts(true, now, emptyList(), 0, emptyList(), 0, 1, null)
        }
    }

    private suspend fun compute(app: TeperaApp, now: Long): OnlineFacts {
        val access = app.balanceRepository.hasUsageAccess()
        val windows = app.sleepWindowRepository.getEnabledWindows()
        val dayStart = app.balanceRepository.calculateDayStartMillis(windows)
        val minutes = if (access) app.balanceRepository.getOnlineMinutes(dayStart, now) else 0
        val intervals = if (access) app.balanceRepository.getOnlineIntervals(dayStart, now) else emptyList()
        return OnlineFacts(
            hasUsageAccess = access,
            dayStartMillis = dayStart,
            windows = windows,
            onlineMinutes = minutes,
            intervals = intervals,
            dayLengthMinutes = app.balanceRepository.calculateDayLengthMinutes(dayStart, windows),
            daySpanMinutes = app.balanceRepository.calculateDaySpanMinutes(dayStart, windows),
            targetMinutes = app.settingsStore.targetMinutes.first()
        )
    }
}
