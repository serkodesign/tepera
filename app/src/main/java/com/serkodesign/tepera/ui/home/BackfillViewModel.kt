package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.GapDetectionConfig
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** T-2 (tepera-dev-spec.md): горизонт бекфілу пауз — та сама глибина, що [PatternViewModel] шукає для патерну. */
const val BACKFILL_LOOKBACK_DAYS = 7

data class BackfillUiState(val isProcessing: Boolean = false)

/**
 * T-2 (tepera-dev-spec.md): "Одразу після надання дозволу обробити всю доступну історію:
 * створити DetectedGapEntity за минулі дні..." — одноразовий бекфіл, що запускається щойно
 * доступ до статистики використання вперше підтверджено (не при першому запуску застосунку —
 * дозвіл зазвичай надається пізніше, через кроки 3/4 онбордингу). `historyBackfillCompletedAt`
 * (`SettingsStore`) гарантує, що це станеться рівно один раз; повторні `runIfNeeded()` (кожен
 * `LifecycleResumeEffect` на Home, як і решта ViewModel-ів) одразу виходять, якщо вже зроблено.
 *
 * Тепловий патерн (`PatternViewModel.availableHistoryDays()`) і структура доби за минулі дні
 * (`StatsViewModel.refreshWeeklyTrend()`) НЕ потребують окремого бекфілу — обидва вже рахують
 * `UsageEvents` на льоту з реальної системної історії, без залежності від `firstLaunchMillis`
 * чи збереженого стану. Лишається зробити персистентним лише те, що саме зберігається в БД:
 * `DetectedGapEntity`.
 */
class BackfillViewModel(
    private val balanceRepository: BalanceRepository,
    private val pauseRepository: PauseRepository,
    private val sleepWindowRepository: SleepWindowRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackfillUiState())
    val uiState: StateFlow<BackfillUiState> = _uiState.asStateFlow()

    init {
        runIfNeeded()
    }

    fun runIfNeeded() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) return@launch
            if (settingsStore.historyBackfillCompletedAt.first() != 0L) return@launch

            // "Обробка не блокує UI довше 2 с, виконується на Dispatchers.IO з індикатором" —
            // сканування й запис уже все на Dispatchers.IO (PauseRepository), тут лише показуємо
            // короткий індикатор на час roботи корутини.
            _uiState.value = BackfillUiState(isProcessing = true)

            val sleepWindows = sleepWindowRepository.getEnabledWindows()
            val config = GapDetectionConfig.forSensitivity(settingsStore.gapSensitivity.first())
            val todayStart = startOfTodayMillis()
            for (daysAgo in 1..BACKFILL_LOOKBACK_DAYS) {
                val dayStart = todayStart - daysAgo * DAY_MILLIS
                val dayEnd = dayStart + DAY_MILLIS
                val scan = pauseRepository.scan(dayStart, dayEnd, sleepWindows, config)
                pauseRepository.persist(dayStart, dayEnd, scan.gaps)
            }

            settingsStore.setHistoryBackfillCompletedAt(System.currentTimeMillis())
            _uiState.value = BackfillUiState(isProcessing = false)
        }
    }

    class Factory(
        private val balanceRepository: BalanceRepository,
        private val pauseRepository: PauseRepository,
        private val sleepWindowRepository: SleepWindowRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BackfillViewModel(balanceRepository, pauseRepository, sleepWindowRepository, settingsStore) as T
    }
}
