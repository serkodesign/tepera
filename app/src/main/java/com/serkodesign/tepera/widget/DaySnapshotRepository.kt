package com.serkodesign.tepera.widget

import com.serkodesign.tepera.data.GapDetectionConfig
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import kotlinx.coroutines.flow.first

/**
 * W-1: збирає [DaySnapshot] із наявних репозиторіїв (той самий пайплайн паузи/Online-хвилин, що
 * вже показує Home/Stats/Pause-картка) — без Context/Room-логіки, це вже лежить у репозиторіях
 * нижче. Не має власного unit-тесту (як і `BalanceRepository`/`PauseRepository` — залежність від
 * `UsageStatsManager`/Room у цьому проєкті тестується лише живим пристроєм); чиста збірка
 * результату — у [buildDaySnapshot], яка й покрита тестами.
 */
class DaySnapshotRepository(
    private val balanceRepository: BalanceRepository,
    private val pauseRepository: PauseRepository,
    private val activityRepository: ActivityRepository,
    private val sleepWindowRepository: SleepWindowRepository,
    private val settingsStore: SettingsStore
) {
    /** Сьогоднішня доба: від точки старту дня до [nowMillis]. */
    suspend fun snapshotForToday(nowMillis: Long = System.currentTimeMillis()): DaySnapshot {
        val sleepWindows = sleepWindowRepository.getEnabledWindows()
        val dayStart = balanceRepository.calculateDayStartMillis(sleepWindows, searchEndMillis = nowMillis)
        return buildFor(dayStart, nowMillis, sleepWindows)
    }

    /** Завершена минула доба: [calendarMidnightMillis] — календарна північ, з якої вона почалась. */
    suspend fun snapshotForPastDay(calendarMidnightMillis: Long): DaySnapshot {
        val sleepWindows = sleepWindowRepository.getEnabledWindows()
        val dayEnd = nextMidnightMillis(calendarMidnightMillis)
        val dayStart = balanceRepository.calculateDayStartMillis(
            sleepWindows,
            referenceMidnightMillis = calendarMidnightMillis,
            searchEndMillis = dayEnd
        )
        return buildFor(dayStart, dayEnd, sleepWindows)
    }

    private suspend fun buildFor(dayStart: Long, dayEnd: Long, sleepWindows: List<SleepWindowEntity>): DaySnapshot {
        // Той самий запобіжник, що WidgetLiveData.gridInputs() — без доступу немає що сканувати,
        // паузи/Online лишаються порожніми, а сам ефір без телефону однаково рахується коректно.
        if (!balanceRepository.hasUsageAccess()) {
            return buildDaySnapshot(dayStart, dayEnd, emptyList(), emptyList(), emptyList(), sleepWindows)
        }
        val config = GapDetectionConfig.forSensitivity(settingsStore.gapSensitivity.first())
        val gaps = pauseRepository.scan(dayStart, dayEnd, sleepWindows, config).gaps
        val entries = activityRepository.observeEntriesInRange(dayStart, dayEnd).first()
        val onlineIntervals = balanceRepository.getOnlineIntervals(dayStart, dayEnd)
        return buildDaySnapshot(dayStart, dayEnd, gaps, entries, onlineIntervals, sleepWindows)
    }
}
