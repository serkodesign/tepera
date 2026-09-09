package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L

/** FR-P.1: три робочі діапазони з тексту SRS ("напр. до 10 год / 10-20 год / понад 20"). */
enum class WeeklyOnlineGuess { UNDER_10, FROM_10_TO_20, OVER_20 }

data class WeeklyReflectionUiState(
    val isDue: Boolean = false,
    val guess: WeeklyOnlineGuess? = null, // не-null => картка в режимі "оцінка → реальність"
    val actualMinutes: Int = 0
)

/**
 * FR-P.1: "оцінка → реальність" — раз на тиждень, необов'язково. Рефлексію запускає власний
 * розрив людини, а не оцінка застосунку: жодного "вище/нижче", лише дві цифри поруч (FR-P.6).
 * Без доступу до статистики використання показувати нема чого — картка мовчить.
 */
class WeeklyReflectionViewModel(
    private val settingsStore: SettingsStore,
    private val balanceRepository: BalanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReflectionUiState())
    val uiState: StateFlow<WeeklyReflectionUiState> = _uiState.asStateFlow()

    init {
        checkDue()
    }

    private fun checkDue() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) return@launch
            val lastHandled = settingsStore.lastReflectionHandledAtMillis.first()
            if (settingsStore.isReflectionDue(lastHandled)) {
                _uiState.value = WeeklyReflectionUiState(isDue = true)
            }
        }
    }

    fun selectGuess(guess: WeeklyOnlineGuess) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val actualMinutes = balanceRepository.getOnlineMinutes(now - WEEK_MILLIS, now)
            _uiState.value = _uiState.value.copy(guess = guess, actualMinutes = actualMinutes)
        }
    }

    /** Викликається і після відповіді ("Гаразд"), і при "Можна пропустити" — обидва закривають картку на тиждень. */
    fun dismiss() {
        viewModelScope.launch {
            settingsStore.setLastReflectionHandledAtMillis(System.currentTimeMillis())
            _uiState.value = WeeklyReflectionUiState(isDue = false)
        }
    }

    class Factory(
        private val settingsStore: SettingsStore,
        private val balanceRepository: BalanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyReflectionViewModel(settingsStore, balanceRepository) as T
    }
}
