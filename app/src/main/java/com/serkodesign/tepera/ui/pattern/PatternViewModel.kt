package com.serkodesign.tepera.ui.pattern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.PatternRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val PATTERN_WINDOW_DAYS = 7 // FR-D.8: "тиждень даних"

data class PatternUiState(
    val visible: Boolean = false,
    val hasEnoughData: Boolean = false,
    val hourlyMinutes: List<Int> = List(24) { 0 }
)

/**
 * FR-D.8/D.9 (SRS v2.6): тепловий патерн доби — Online-хвилини по годинах, за останні 7 ПОВНИХ
 * календарних днів (без сьогоднішнього — частковий день перекосив би патерн у бік ранніх годин).
 * Спільний для компактної картки на Home (`PatternMiniCard`) і повної картки на Stats — обидва
 * екрани створюють свій власний інстанс через Factory, кожен зі своїм refresh-циклом.
 *
 * "Тиждень даних" (FR-D.9) — від ПЕРШОГО ЗАПУСКУ застосунку (`SettingsStore.firstLaunchMillis`),
 * не від першої появи даних у `UsageStatsManager` (системна історія існує незалежно від
 * встановлення Tepera й не є надійним сигналом "користувач уже тиждень з нами").
 */
class PatternViewModel(
    private val patternRepository: PatternRepository,
    private val balanceRepository: BalanceRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatternUiState())
    val uiState: StateFlow<PatternUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) {
                _uiState.value = PatternUiState()
                return@launch
            }

            val firstLaunch = settingsStore.firstLaunchMillis.first()
            val daysSinceFirstLaunch = (System.currentTimeMillis() - firstLaunch) / DAY_MILLIS
            if (daysSinceFirstLaunch < PATTERN_WINDOW_DAYS) {
                _uiState.value = PatternUiState(visible = true, hasEnoughData = false)
                return@launch
            }

            val to = startOfTodayMillis() // виключно повні дні — сьогоднішній частковий день не рахується
            val from = to - PATTERN_WINDOW_DAYS * DAY_MILLIS
            val buckets = patternRepository.hourlyOnlineMinutes(from, to)
            _uiState.value = PatternUiState(visible = true, hasEnoughData = true, hourlyMinutes = buckets)
        }
    }

    class Factory(
        private val patternRepository: PatternRepository,
        private val balanceRepository: BalanceRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PatternViewModel(patternRepository, balanceRepository, settingsStore) as T
    }
}
