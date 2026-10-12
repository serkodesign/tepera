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
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val MIN_DAYS_BEFORE_FIRST_SHOW = 7
private const val MEDIAN_WINDOW_DAYS = 7
private const val MIN_DAYS_WITH_DATA = 5 // "потрібні дані мінімум за 5 із 7 останніх днів"

/**
 * T-10 (tepera-dev-spec.md): "вибір діапазону" для "О котрій ти вчора востаннє брав телефон?" —
 * [minutesBeforeBoundary] кодує кожен варіант у координатах "хвилин до 02:00-межі" (FR-D.7a),
 * а не "хвилина доби": ці варіанти перетинають північ (23:00–00:00, 00:00–01:00), і пряме
 * порівняння хвилин-доби 0-1439 дало б хибний порядок навколо півночі. У цих координатах більше
 * значення = раніше відклав телефон, монотонно й без розриву на межі півночі.
 */
enum class LastPhoneUseGuess(val minutesBeforeBoundary: Long) {
    BEFORE_22(240),      // "До 22:00" — представник 22:00 (02:00 - 22:00 = 4 год)
    FROM_22_TO_23(210),  // представник 22:30
    FROM_23_TO_00(150),  // представник 23:30
    FROM_00_TO_01(90),   // представник 00:30
    AFTER_01(30);        // "Після 01:00" — представник 01:30

    companion object {
        fun fromMinutesBeforeBoundary(value: Long): LastPhoneUseGuess? =
            entries.find { it.minutesBeforeBoundary == value }
    }
}

data class LastPhoneUseEstimateUiState(
    val isDue: Boolean = false,
    val guess: LastPhoneUseGuess? = null,
    val actualMillis: Long? = null,
    /** Другим рядком, тихіше, без підпису (документ) — медіана за останні [MEDIAN_WINDOW_DAYS] днів. */
    val medianMillis: Long? = null
)

/**
 * T-10 (tepera-dev-spec.md): замінює прибраний ПОСТІЙНИЙ показ "востаннє брав телефон о HH:MM"
 * на Home (`LastPhoneUseCard`, FR-D.7, v2.8 — видалено, розділ 2.2 "принцип пасивного сорому"
 * забороняє пасивний показ числа, здатного засмутити) — той самий формат "оцінка → реальність",
 * що [WeeklyReflectionCard]/[UnlockEstimateViewModel]. Сире число й далі доступне на Stats
 * ("деталі дня"/"тижневий огляд", `StatsViewModel`), звичайним рядком.
 *
 * **T-13 (tepera-dev-spec.md), "рушій карток": [isDue] тепер означає лише "готовність даних"**
 * (доступ + минуло [MIN_DAYS_BEFORE_FIRST_SHOW] днів від першого запуску + дані мінімум за
 * [MIN_DAYS_WITH_DATA] із [MEDIAN_WINDOW_DAYS] останніх 02:00-зсунутих діб) — не "показати прямо
 * зараз". Мінімальний інтервал повтору і правило "ніколи в тому самому тижні, що тижнева
 * рефлексія" тепер — робота `CardEngine`, той самий перехід, що [UnlockEstimateViewModel].
 * **Не залежить від того, число пізнє чи раннє** — жодна з умов вище не звіряється з фактичним
 * часом, лише з наявністю даних і календарем показів (документ прямо попереджає: інакше картка
 * стає "адресною доставкою сорому" тим, хто лягає пізно).
 */
class LastPhoneUseEstimateViewModel(
    private val pauseRepository: PauseRepository,
    private val balanceRepository: BalanceRepository,
    private val userEstimateRepository: UserEstimateRepository,
    private val settingsStore: SettingsStore,
    private val cardHistoryRepository: CardHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LastPhoneUseEstimateUiState())
    val uiState: StateFlow<LastPhoneUseEstimateUiState> = _uiState.asStateFlow()

    init {
        checkDataReady()
    }

    private fun checkDataReady() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) return@launch

            val firstLaunch = settingsStore.firstLaunchMillis.first()
            val now = System.currentTimeMillis()
            if (now - firstLaunch < MIN_DAYS_BEFORE_FIRST_SHOW * DAY_MILLIS) return@launch

            val daysWithData = (0 until MEDIAN_WINDOW_DAYS).count { daysBack ->
                pauseRepository.lastPhoneUseForShiftedDay(now, daysBack).lastUseMillis != null
            }
            if (daysWithData < MIN_DAYS_WITH_DATA) return@launch

            _uiState.value = LastPhoneUseEstimateUiState(isDue = true)
        }
    }

    fun selectGuess(guess: LastPhoneUseGuess) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val yesterday = pauseRepository.lastPhoneUseForShiftedDay(now, daysBack = 0)
            val actualMillis = yesterday.lastUseMillis
            if (actualMillis == null) {
                // Рідкісний крайовий випадок: дані зникли між checkDataReady() (перевіряв 5 з 7
                // днів загалом) і вибором (саме "вчора" без даних) — тихо закриваємо, без порожньої картки.
                dismiss()
                return@launch
            }
            val actualMinutesBeforeBoundary = (yesterday.boundaryMillis - actualMillis) / 60_000L

            userEstimateRepository.saveResolvedEstimate(
                type = EstimateType.LAST_PHONE_USE,
                estimatedValue = guess.minutesBeforeBoundary,
                actualValue = actualMinutesBeforeBoundary,
                forDate = LocalDate.now().minusDays(1)
            )

            val median = pauseRepository.medianLastPhoneUseMillis(now, MEDIAN_WINDOW_DAYS)
            _uiState.value = _uiState.value.copy(guess = guess, actualMillis = actualMillis, medianMillis = median)
        }
    }

    /** Викликається і після відповіді ("Гаразд"), і при "Можна пропустити" — обидва зсувають наступну появу на 2 тижні. */
    fun dismiss() {
        viewModelScope.launch {
            val result = if (_uiState.value.guess != null) CardResult.ANSWERED else CardResult.SKIPPED
            cardHistoryRepository.recordResolved(CardType.LAST_PHONE_USE_ESTIMATE, result)
            _uiState.value = LastPhoneUseEstimateUiState()
        }
    }

    class Factory(
        private val pauseRepository: PauseRepository,
        private val balanceRepository: BalanceRepository,
        private val userEstimateRepository: UserEstimateRepository,
        private val settingsStore: SettingsStore,
        private val cardHistoryRepository: CardHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LastPhoneUseEstimateViewModel(
                pauseRepository, balanceRepository, userEstimateRepository,
                settingsStore, cardHistoryRepository
            ) as T
    }
}
