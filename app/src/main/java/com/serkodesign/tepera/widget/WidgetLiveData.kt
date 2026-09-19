package com.serkodesign.tepera.widget

import com.serkodesign.tepera.TeperaApp
import kotlinx.coroutines.flow.MutableStateFlow

/** Важка (сканування UsageEvents) частина даних сітки доби — кешується, див. [WidgetLiveData]. */
class GridInputs(
    val hasUsageAccess: Boolean,
    val dayStartMillis: Long,
    val onlineMinutesPerSlot: IntArray
)

/**
 * Живі дані віджета. **Причина, з якої це існує (знайдено на Samsung S23):** раніше `provideGlance()`
 * завантажував усе (таймери, категорії, записи, Online-хвилини) ОДИН РАЗ на початку сесії Glance,
 * а сесія живе ще деякий час після оновлення — наступні `update()` лише перекомпоновували той самий
 * знімок. Наслідки: другий і далі тап показував застарілий стан кнопок, а два екземпляри віджета
 * (окремі сесії різного віку) розходились між собою. Плюс кожна нова сесія заново сканувала
 * `UsageEvents` (дорого) — відгук на тап був повільним.
 *
 * Тепер таймери/категорії/записи — потоки (`collectAsState`) усередині композиції, тож будь-яка
 * жива сесія оновлюється сама, щойно змінюється сховище; важка частина ([gridInputs]) кешується
 * на 5 хв і скидається лише [forceRefresh] (WidgetUpdateWorker, ~30 хв).
 */
object WidgetLiveData {
    private const val CACHE_MILLIS = 5 * 60_000L

    /** Піднімається [forceRefresh]; сесії залежать від нього й перераховують важку частину. */
    val refreshTick = MutableStateFlow(0L)

    private var cached: GridInputs? = null
    private var cachedAt = 0L
    private var cachedMidnight = 0L

    fun forceRefresh() {
        cached = null
        refreshTick.value++
    }

    suspend fun gridInputs(app: TeperaApp, midnightMillis: Long): GridInputs {
        val now = System.currentTimeMillis()
        cached?.let { c ->
            if (cachedMidnight == midnightMillis && now - cachedAt < CACHE_MILLIS) return c
        }
        val hasUsageAccess = app.balanceRepository.hasUsageAccess()
        // SRS v2.5, FR-3.5: точка старту дня замінює локальну північ — та сама логіка, що на Home.
        // Тут вона позначає лише межу PreUnlock-клітинок; сама сітка анкерується на календарну північ.
        val sleepWindows = app.sleepWindowRepository.getEnabledWindows()
        val dayStart = app.balanceRepository.calculateDayStartMillis(sleepWindows)
        val online = if (hasUsageAccess) {
            app.balanceRepository.getOnlineMinutesPerSlot(
                fromMillis = midnightMillis,
                slotMinutes = 30,
                slotCount = DAILY_GRID_SLOT_COUNT
            )
        } else {
            IntArray(DAILY_GRID_SLOT_COUNT)
        }
        return GridInputs(hasUsageAccess, dayStart, online).also {
            cached = it
            cachedAt = now
            cachedMidnight = midnightMillis
        }
    }
}
