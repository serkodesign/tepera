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
private const val MIN_HISTORY_DAYS_FOR_PATTERN = 2 // T-2 (tepera-dev-spec.md): "історії менше 2 днів — FR-D.9"

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
 * **"Тиждень даних" (FR-D.9) — СКАСОВАНО в T-2 (tepera-dev-spec.md, "бекфіл історії при першому
 * запуску").** Раніше поріг рахувався від ПЕРШОГО ЗАПУСКУ застосунку (`SettingsStore.
 * firstLaunchMillis`), свідомо НЕ від першої появи даних у `UsageStatsManager` — те рішення
 * явно обґрунтовувало це так: "системна історія існує незалежно від встановлення Tepera й не є
 * надійним сигналом 'користувач уже тиждень з нами'". T-2 прямо вимагає протилежного: "одразу
 * після надання дозволу обробити всю доступну історію... видно патерн, а не порожній стан" —
 * системна історія ТЕПЕР навмисно вважається достатнім сигналом, бо цінність продукту саме в
 * тому, щоб показати вже наявний (невидимий людині) патерн, а не змушувати чекати довільний
 * тиждень, поки БД накопичить власні дані. Поріг тепер — [PatternRepository.availableHistoryDays]
 * (щонайменше [MIN_HISTORY_DAYS_FOR_PATTERN] дні реальної історії `UsageEvents`, не дні з
 * інсталяції). `firstLaunchMillis` лишається чинним для інших порогів (`WeeklyDigestViewModel`),
 * T-2 змінює гейтинг лише тут.
 *
 * **Закриття картки, якщо прочитав (за прямим запитом користувача, не в SRS):** ключ закриття —
 * `to` (початок сьогоднішньої доби, той самий, що визначає вікно патерну) — закриття діє, доки
 * не почнеться нова доба, тоді картка повертається з оновленим вікном.
 */
class PatternViewModel(
    private val patternRepository: PatternRepository,
    private val balanceRepository: BalanceRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatternUiState())
    val uiState: StateFlow<PatternUiState> = _uiState.asStateFlow()

    private var currentDismissKey: Long = -1L

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) {
                _uiState.value = PatternUiState()
                return@launch
            }

            // Ключ закриття — сьогоднішня доба, той самий і в "ще збираємо дані", і в готовому
            // патерні нижче: "закрито" діє рівно до завтра, незалежно від того, який зі станів
            // картка показувала на момент закриття.
            val to = startOfTodayMillis()
            currentDismissKey = to
            val dismissedKey = settingsStore.patternCardDismissedKey.first()
            if (dismissedKey == to) {
                _uiState.value = PatternUiState()
                return@launch
            }

            val now = System.currentTimeMillis()
            val availableDays = patternRepository.availableHistoryDays(now, PATTERN_WINDOW_DAYS)
            if (availableDays < MIN_HISTORY_DAYS_FOR_PATTERN) {
                _uiState.value = PatternUiState(visible = true, hasEnoughData = false)
                return@launch
            }

            // Використовує ВСЮ доступну історію (до PATTERN_WINDOW_DAYS), не завжди рівно 7 днів —
            // патерн одразу видимий і самопоправляється, як тільки накопичується більше днів.
            val from = to - availableDays * DAY_MILLIS
            val buckets = patternRepository.hourlyOnlineMinutes(from, to)
            _uiState.value = PatternUiState(visible = true, hasEnoughData = true, hourlyMinutes = buckets)
        }
    }

    /** Закрити картку до наступної доби — `currentDismissKey` завжди відповідає останньому [refresh]. */
    fun dismiss() {
        viewModelScope.launch {
            settingsStore.setPatternCardDismissedKey(currentDismissKey)
            _uiState.value = PatternUiState()
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
