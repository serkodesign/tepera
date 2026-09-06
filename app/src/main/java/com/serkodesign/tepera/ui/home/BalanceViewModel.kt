package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** hasUsageAccess == null означає, що перевірка ще не завершилась (перший рендер). */
data class BalanceUiState(
    val hasUsageAccess: Boolean? = null,
    val onlineMinutes: Int = 0,
    val offlineMinutes: Int = 0,
    val targetMinutes: Int = 180,
    val denominatorMinutes: Int = 180
)

/**
 * FR-3.1–3.6: об'єднує Online-хвилини (опитуються поштучно, UsageStatsManager не має live-потоку),
 * Offline-хвилини (реактивно з ActivityRepository) і таргет (реактивно з SettingsStore).
 */
class BalanceViewModel(
    private val balanceRepository: BalanceRepository,
    activityRepository: ActivityRepository,
    settingsStore: SettingsStore
) : ViewModel() {

    private val hasUsageAccess = MutableStateFlow<Boolean?>(null)
    private val onlineMinutes = MutableStateFlow(0)

    val uiState: StateFlow<BalanceUiState> = combine(
        hasUsageAccess,
        onlineMinutes,
        activityRepository.observeEntriesInRange(startOfTodayMillis(), Long.MAX_VALUE),
        settingsStore.targetMinutes
    ) { access, online, entries, target ->
        BalanceUiState(
            hasUsageAccess = access,
            onlineMinutes = online,
            offlineMinutes = entries.sumOf { it.durationMinutes },
            targetMinutes = target,
            denominatorMinutes = balanceRepository.calculateDenominatorMinutes()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalanceUiState())

    init {
        refresh()
    }

    /** Викликається при вході на Home і при поверненні з системних Налаштувань (LifecycleResumeEffect). */
    fun refresh() {
        viewModelScope.launch {
            val access = balanceRepository.hasUsageAccess()
            hasUsageAccess.value = access
            if (access) {
                onlineMinutes.value = balanceRepository.getOnlineMinutesToday()
            }
        }
    }

    class Factory(
        private val balanceRepository: BalanceRepository,
        private val activityRepository: ActivityRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BalanceViewModel(balanceRepository, activityRepository, settingsStore) as T
    }
}
