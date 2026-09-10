package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val WEEK_MILLIS = 7 * DAY_MILLIS
private const val DIGEST_WINDOW_DAYS = 7

data class WeeklyDigestUiState(
    val visible: Boolean = false,
    val readingCount: Int = 0,
    val readingMinutes: Int = 0,
    val movementCount: Int = 0,
    /** Хвилини від півночі, null — якщо за тиждень жодного дня не вдалось визначити точку старту. */
    val dayUsuallyStartsMinuteOfDay: Int? = null
)

/**
 * Досліджений у Figma-макеті (node 2062:2862, "This week") тип картки — кількісний тижневий
 * дайджест, на відміну від "Патерну доби" (FR-D.8, коли саме) чи тижневої рефлексії (FR-P.1,
 * оцінка vs реальність). НЕ в SRS v2.6/v2.7 буквально — додано за прямим запитом користувача
 * після спільного розбору макета.
 *
 * СВІДОМА ЗМІНА відносно макета: мокап показував "Хобі: 3 з 4 тижнів" — метрику кадансу/стріку.
 * Це прямо суперечить Monastic Style ("без бейджів, без стріків", CLAUDE.md, розділ "Дизайн-
 * філософія") і принципу FR-P.3 ("жодного змагання з собою через послідовність"). Замінено на
 * прості абсолютні лічильники (кількість записів і сумарний час), як і решта картки — узгоджується
 * з FR-P.6 ("число завжди з контекстом, ніколи голе"), без цифри, що натякає на пропущений день.
 *
 * "Тиждень даних" (FR-D.9-подібний поріг) — від `SettingsStore.firstLaunchMillis`, той самий
 * підхід, що й `PatternViewModel`. За прямим запитом користувача картка ХОВАЄТЬСЯ ЦІЛКОМ, поки
 * даних менше тижня, а не показує плейсхолдер "ще збираємо дані" (на відміну від `PatternMiniCard`,
 * де такий текст — явна вимога FR-D.9; тут це не вимога SRS, тож "нема актуальної інформації —
 * немає картки" застосовується буквально).
 *
 * **Закриття картки, якщо прочитав (за прямим запитом користувача, не в SRS):** ключ закриття —
 * сьогоднішня доба — діє, доки не почнеться нова доба.
 */
class WeeklyDigestViewModel(
    private val activityRepository: ActivityRepository,
    private val balanceRepository: BalanceRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyDigestUiState())
    val uiState: StateFlow<WeeklyDigestUiState> = _uiState.asStateFlow()

    private var currentDismissKey: Long = -1L

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val to = startOfTodayMillis()
            currentDismissKey = to
            val dismissedKey = settingsStore.weeklyDigestCardDismissedKey.first()
            if (dismissedKey == to) {
                _uiState.value = WeeklyDigestUiState()
                return@launch
            }

            val firstLaunch = settingsStore.firstLaunchMillis.first()
            val daysSinceFirstLaunch = (System.currentTimeMillis() - firstLaunch) / DAY_MILLIS
            if (daysSinceFirstLaunch < DIGEST_WINDOW_DAYS) {
                _uiState.value = WeeklyDigestUiState() // нема ще актуальної інформації — картка мовчить
                return@launch
            }

            val now = System.currentTimeMillis()
            val entries = activityRepository.observeEntriesInRange(now - WEEK_MILLIS, now).first()
            val readingEntries = entries.filter { it.categoryId == DefaultCategories.READING_ID }
            val movementCount = entries.count { it.categoryId == DefaultCategories.MOVEMENT_ID }

            val dayStart = averageDayStartMinuteOfDay()

            val readingCount = readingEntries.size
            val readingMinutes = readingEntries.sumOf { it.durationMinutes }
            val hasAnyMetric = readingCount > 0 || movementCount > 0 || dayStart != null

            // Якщо буквально нічого не сталось за тиждень — картка мовчить, а не показує нулі
            // (FR-3.4-подібний принцип: незалогованість ніколи не подається як докір).
            _uiState.value = WeeklyDigestUiState(
                visible = hasAnyMetric,
                readingCount = readingCount,
                readingMinutes = readingMinutes,
                movementCount = movementCount,
                dayUsuallyStartsMinuteOfDay = dayStart
            )
        }
    }

    /** Закрити картку до наступної доби — `currentDismissKey` завжди відповідає останньому [refresh]. */
    fun dismiss() {
        viewModelScope.launch {
            settingsStore.setWeeklyDigestCardDismissedKey(currentDismissKey)
            _uiState.value = WeeklyDigestUiState()
        }
    }

    /**
     * Середня точка старту дня (FR-3.5-формула, `BalanceRepository.calculateDayStartMillis`) за
     * останні [DIGEST_WINDOW_DAYS] ПОВНИХ календарних днів — той самий алгоритм, що визначає
     * "твій день з телефоном" (FR-D.6), просто застосований по одному разу на кожну минулу добу.
     * Дні без жодного знайденого "суттєвого" розблокування (сентинел — результат == searchEndMillis)
     * пропускаються з усереднення, а не рахуються як північ.
     */
    private suspend fun averageDayStartMinuteOfDay(): Int? {
        if (!balanceRepository.hasUsageAccess()) return null
        val sleepWindowEndHour = settingsStore.sleepWindowEndHour.first()
        val todayStart = startOfTodayMillis()

        val minutesOfDay = (1..DIGEST_WINDOW_DAYS).mapNotNull { daysAgo ->
            val midnight = todayStart - daysAgo * DAY_MILLIS
            val nextMidnight = midnight + DAY_MILLIS
            val dayStart = balanceRepository.calculateDayStartMillis(sleepWindowEndHour, midnight, nextMidnight)
            if (dayStart >= nextMidnight) return@mapNotNull null // нічого не знайдено цього дня
            ((dayStart - midnight) / 60_000L).toInt()
        }
        if (minutesOfDay.isEmpty()) return null
        return minutesOfDay.sum() / minutesOfDay.size
    }

    class Factory(
        private val activityRepository: ActivityRepository,
        private val balanceRepository: BalanceRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyDigestViewModel(activityRepository, balanceRepository, settingsStore) as T
    }
}
