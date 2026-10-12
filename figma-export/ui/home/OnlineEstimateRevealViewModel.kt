package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.EstimateType
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class OnlineEstimateRevealUiState(
    val visible: Boolean = false,
    val estimatedMinutes: Long = 0,
    val actualMinutes: Long = 0
)

/**
 * T-3 (tepera-dev-spec.md), розділ 2.2 "принцип пасивного сорому": показує "твоя оцінка / реальне
 * число" ОДИН РАЗ, щойно з'являється доступ до статистики використання — незалежно від того, чи
 * це сталось одразу після онбординг-кроку 4 (система Налаштування → назад на Home), чи набагато
 * пізніше (акцептанс-критерій T-3: "дозвіл не надано → оцінка зберігається і показується разом із
 * фактом пізніше"). Обидва випадки покриває той самий механізм: [refresh] шукає найновіший
 * нерозв'язаний `UserEstimateEntity` (ONLINE_HOURS) щоразу, коли відкривається Home.
 */
class OnlineEstimateRevealViewModel(
    private val userEstimateRepository: UserEstimateRepository,
    private val balanceRepository: BalanceRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineEstimateRevealUiState())
    val uiState: StateFlow<OnlineEstimateRevealUiState> = _uiState.asStateFlow()

    private var currentEstimateId: String? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) {
                _uiState.value = OnlineEstimateRevealUiState()
                return@launch
            }
            val estimate = userEstimateRepository.getLatestUnresolved(EstimateType.ONLINE_HOURS)
            if (estimate == null) {
                _uiState.value = OnlineEstimateRevealUiState()
                return@launch
            }
            if (settingsStore.onlineEstimateRevealDismissedId.first() == estimate.id) {
                _uiState.value = OnlineEstimateRevealUiState()
                return@launch
            }
            currentEstimateId = estimate.id
            val actualMinutes = estimate.actualValue ?: run {
                val dayStart = estimate.forDate.startOfDayMillis()
                val dayEnd = estimate.forDate.plusDays(1).startOfDayMillis()
                val computed = balanceRepository.getOnlineMinutes(dayStart, dayEnd).toLong()
                userEstimateRepository.setActualValue(estimate.id, computed)
                computed
            }
            _uiState.value = OnlineEstimateRevealUiState(
                visible = true,
                estimatedMinutes = estimate.estimatedValue,
                actualMinutes = actualMinutes
            )
        }
    }

    /** Той самий принцип "×", що інші картки — приховує розкриття назавжди для цього рядка. */
    fun dismiss() {
        viewModelScope.launch {
            currentEstimateId?.let { settingsStore.setOnlineEstimateRevealDismissedId(it) }
            _uiState.value = OnlineEstimateRevealUiState()
        }
    }

    private fun LocalDate.startOfDayMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    class Factory(
        private val userEstimateRepository: UserEstimateRepository,
        private val balanceRepository: BalanceRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnlineEstimateRevealViewModel(userEstimateRepository, balanceRepository, settingsStore) as T
    }
}
