package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.cards.CardResult
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val MIN_DAYS_BEFORE_FIRST_SHOW = 7

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
 *
 * **T-13 (tepera-dev-spec.md), "рушій карток": [isDue] тепер означає лише "готовність даних"**
 * (доступ наданий + минув мінімум [MIN_DAYS_BEFORE_FIRST_SHOW] днів від першого запуску — той
 * самий поріг, що вже мали [UnlockEstimateViewModel]/[LastPhoneUseEstimateViewModel], тепер
 * застосований і тут для одноманітності), А НЕ "показати прямо зараз". Чи справді показати —
 * вирішує `CardEngine` (мінімальний інтервал повтору 7 днів, рахований від [CardHistoryRepository]
 * замість колишнього `SettingsStore.lastReflectionHandledAtMillis`, і глобальний бюджет "не
 * більше 1 картки-оцінки на тиждень, сумарно по всіх типах" — раніше цю умову вручну звіряли
 * [UnlockEstimateViewModel]/[LastPhoneUseEstimateViewModel] проти саме цього поля, тепер рушій
 * робить це для БУДЬ-ЯКОЇ картки-оцінки автоматично).
 */
class WeeklyReflectionViewModel(
    private val settingsStore: SettingsStore,
    private val balanceRepository: BalanceRepository,
    private val cardHistoryRepository: CardHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReflectionUiState())
    val uiState: StateFlow<WeeklyReflectionUiState> = _uiState.asStateFlow()

    init {
        checkDataReady()
    }

    private fun checkDataReady() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) return@launch
            val firstLaunch = settingsStore.firstLaunchMillis.first()
            val now = System.currentTimeMillis()
            if (now - firstLaunch < MIN_DAYS_BEFORE_FIRST_SHOW * DAY_MILLIS) return@launch
            _uiState.value = WeeklyReflectionUiState(isDue = true)
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
            val result = if (_uiState.value.guess != null) CardResult.ANSWERED else CardResult.SKIPPED
            cardHistoryRepository.recordResolved(CardType.WEEKLY_REFLECTION, result)
            _uiState.value = WeeklyReflectionUiState(isDue = false)
        }
    }

    class Factory(
        private val settingsStore: SettingsStore,
        private val balanceRepository: BalanceRepository,
        private val cardHistoryRepository: CardHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyReflectionViewModel(settingsStore, balanceRepository, cardHistoryRepository) as T
    }
}
