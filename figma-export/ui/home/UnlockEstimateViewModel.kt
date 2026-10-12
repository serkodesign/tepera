package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.cards.CardResult
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.EstimateType
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val MIN_DAYS_BEFORE_FIRST_SHOW = 7

/** T-14: "вибір діапазону" — той самий принцип, що [com.serkodesign.tepera.ui.onboarding.DailyOnlineGuess]. */
enum class UnlockCountGuess(val representativeCount: Long) {
    UNDER_30(15),
    THIRTY_TO_70(50),
    OVER_70(90);

    companion object {
        fun fromRepresentativeCount(count: Long): UnlockCountGuess? =
            entries.find { it.representativeCount == count }
    }
}

data class UnlockEstimateUiState(
    val isDue: Boolean = false,
    val guess: UnlockCountGuess? = null,
    val actualCount: Long = 0
)

/**
 * T-14 (tepera-dev-spec.md), розділ 2.2 "принцип пасивного сорому": кількість розблокувань за
 * добу — "класичне число-сором", тому НІКОЛИ не подається пасивно (Home/віджет) — лише через
 * періодичну картку "оцінка → реальність", той самий формат, що [WeeklyReflectionViewModel].
 *
 * **T-13 (tepera-dev-spec.md), "рушій карток": [isDue] тепер означає лише "готовність даних"**
 * (підтримка API + доступ + минуло [MIN_DAYS_BEFORE_FIRST_SHOW] днів від першого запуску) — не
 * "показати прямо зараз". Мінімальний інтервал повтору (раніше "не частіше разу на 2 тижні",
 * вручну звірений проти `SettingsStore.lastUnlockEstimateHandledMillis`) і правило "ніколи в
 * тому самому тижні, що тижнева рефлексія" (раніше вручну звірений проти
 * `SettingsStore.lastReflectionHandledAtMillis`) тепер обидва — робота `CardEngine`: перше через
 * `minIntervalDays = 14` у джерелі картки, друге — автоматично, глобальним бюджетом "не більше
 * 1 картки-оцінки на тиждень" (діє для БУДЬ-якої картки з `CardType.isEstimate`, не лише цієї
 * пари вручну).
 *
 * Розблокування всередині вікна сну (T-12) не рахуються — `UnlockRepository.countUnlocks()`.
 */
class UnlockEstimateViewModel(
    private val unlockRepository: UnlockRepository,
    private val balanceRepository: BalanceRepository,
    private val sleepWindowRepository: SleepWindowRepository,
    private val userEstimateRepository: UserEstimateRepository,
    private val settingsStore: SettingsStore,
    private val cardHistoryRepository: CardHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockEstimateUiState())
    val uiState: StateFlow<UnlockEstimateUiState> = _uiState.asStateFlow()

    init {
        checkDataReady()
    }

    private fun checkDataReady() {
        viewModelScope.launch {
            if (!unlockRepository.isSupported() || !balanceRepository.hasUsageAccess()) return@launch

            val firstLaunch = settingsStore.firstLaunchMillis.first()
            val now = System.currentTimeMillis()
            if (now - firstLaunch < MIN_DAYS_BEFORE_FIRST_SHOW * DAY_MILLIS) return@launch

            _uiState.value = UnlockEstimateUiState(isDue = true)
        }
    }

    /** Реальне значення рахується одразу (на відміну від T-3 — доступ тут уже гарантовано наданий). */
    fun selectGuess(guess: UnlockCountGuess) {
        viewModelScope.launch {
            val yesterdayEnd = startOfTodayMillis()
            val yesterdayStart = yesterdayEnd - DAY_MILLIS
            val sleepWindows = sleepWindowRepository.getEnabledWindows()
            val actual = unlockRepository.countUnlocks(yesterdayStart, yesterdayEnd, sleepWindows).toLong()
            userEstimateRepository.saveResolvedEstimate(
                type = EstimateType.UNLOCK_COUNT,
                estimatedValue = guess.representativeCount,
                actualValue = actual,
                forDate = LocalDate.now().minusDays(1)
            )
            _uiState.value = _uiState.value.copy(guess = guess, actualCount = actual)
        }
    }

    /** Викликається і після відповіді ("Гаразд"), і при "Можна пропустити" — обидва зсувають наступну появу на 2 тижні. */
    fun dismiss() {
        viewModelScope.launch {
            val result = if (_uiState.value.guess != null) CardResult.ANSWERED else CardResult.SKIPPED
            cardHistoryRepository.recordResolved(CardType.UNLOCK_ESTIMATE, result)
            _uiState.value = UnlockEstimateUiState()
        }
    }

    class Factory(
        private val unlockRepository: UnlockRepository,
        private val balanceRepository: BalanceRepository,
        private val sleepWindowRepository: SleepWindowRepository,
        private val userEstimateRepository: UserEstimateRepository,
        private val settingsStore: SettingsStore,
        private val cardHistoryRepository: CardHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UnlockEstimateViewModel(
                unlockRepository, balanceRepository, sleepWindowRepository,
                userEstimateRepository, settingsStore, cardHistoryRepository
            ) as T
    }
}
