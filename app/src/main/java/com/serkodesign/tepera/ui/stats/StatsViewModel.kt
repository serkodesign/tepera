package com.serkodesign.tepera.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MS_PER_DAY = 24 * 60 * 60 * 1000L

/** FR-5.2: період для стовпчикової діаграми розподілу офлайн-часу по категоріях. */
enum class StatsPeriod { DAY, WEEK, MONTH }

data class CategoryBreakdownItem(val category: CategoryEntity, val minutes: Int)

/**
 * FR-5.3: одна точка тижневого тренду — доба + абсолютні Online-хвилини на неї.
 * FR-P.6 (SRS v2.5): свідомо НЕ частка/відсоток від знаменника Grace Period Buffer — голий %
 * без контексту читається як оцінка, а не факт. Абсолютний час порівнюється сам із собою день
 * до дня, без прихованого "буфера справедливості", який мав сенс лише для Home-шкали сьогодні.
 */
data class DailyBalancePoint(val dayStartMillis: Long, val onlineMinutes: Int)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val weeklyTrend: List<DailyBalancePoint> = emptyList(),
    val hasUsageAccess: Boolean = true
)

/**
 * FR-5.2, FR-5.3: екран "Статистика". Розподіл по категоріях — реактивний (та сама модель, що
 * й Home), тижневий тренд балансу — опитується поштучно (UsageStatsManager без live-потоку),
 * так само як BalanceViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val categoryRepository: CategoryRepository,
    private val activityRepository: ActivityRepository,
    private val balanceRepository: BalanceRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.WEEK)
    private val weeklyTrend = MutableStateFlow<List<DailyBalancePoint>>(emptyList())
    private val hasUsageAccess = MutableStateFlow(true)

    // "Тиждень"/"Місяць" — ковзне вікно останніх 7/30 днів, не календарний тиждень/місяць
    // (спрощення MVP, узгоджується з відомим timezone/календарним обмеженням SRS розділ 11).
    private fun periodStartMillis(p: StatsPeriod): Long = when (p) {
        StatsPeriod.DAY -> startOfTodayMillis()
        StatsPeriod.WEEK -> startOfTodayMillis() - 6 * MS_PER_DAY
        StatsPeriod.MONTH -> startOfTodayMillis() - 29 * MS_PER_DAY
    }

    private val categoryBreakdown: StateFlow<List<CategoryBreakdownItem>> = period
        .flatMapLatest { p ->
            combine(
                categoryRepository.observeActiveCategories(),
                activityRepository.observeEntriesInRange(periodStartMillis(p), Long.MAX_VALUE)
            ) { categories, entries ->
                categories.sortedBy { it.sortOrder }.map { category ->
                    val minutes = entries.filter { it.categoryId == category.id }.sumOf { it.durationMinutes }
                    CategoryBreakdownItem(category, minutes)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<StatsUiState> = combine(
        period,
        categoryBreakdown,
        weeklyTrend,
        hasUsageAccess
    ) { p, breakdown, trend, access ->
        StatsUiState(p, breakdown, trend, access)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    init {
        refreshWeeklyTrend()
    }

    fun selectPeriod(p: StatsPeriod) {
        period.value = p
    }

    /** Викликається при вході на екран і при поверненні з системних Налаштувань. */
    fun refreshWeeklyTrend() {
        viewModelScope.launch {
            val access = balanceRepository.hasUsageAccess()
            hasUsageAccess.value = access
            if (!access) {
                weeklyTrend.value = emptyList()
                return@launch
            }
            val todayStart = startOfTodayMillis()
            // FR-3.5 (SRS v2.5): та сама точка старту дня, що на Home (BalanceViewModel.refresh())
            // — перше суттєве розблокування після вікна сну, не локальна північ. Рахується один
            // раз тут, бо стосується лише сьогоднішньої (daysAgo == 0) точки тренду.
            val sleepWindowEndHour = settingsStore.sleepWindowEndHour.first()
            val todayDayStart = balanceRepository.calculateDayStartMillis(sleepWindowEndHour)
            val points = (6 downTo 0).map { daysAgo ->
                // Для сьогодні (daysAgo == 0) день ще не завершився — межа старту та сама точка
                // старту дня, що на Home, не північ. Для минулих завершених діб — календарна доба
                // [північ, наступна північ). Без ділення на знаменник (FR-P.6 вище) — просто
                // абсолютні хвилини Online за цю добу.
                val dayStart = if (daysAgo == 0) todayDayStart else todayStart - daysAgo * MS_PER_DAY
                val dayEnd = if (daysAgo == 0) System.currentTimeMillis() else dayStart + MS_PER_DAY
                val onlineMinutes = balanceRepository.getOnlineMinutes(dayStart, dayEnd)
                // Точка на графіку лишається прив'язана до календарного дня (todayStart - daysAgo
                // * MS_PER_DAY), не до фактичної точки старту дня — інакше вісь X "тижневого
                // тренду" сьогоднішньої точки зсувалась би вбік від решти днів.
                DailyBalancePoint(todayStart - daysAgo * MS_PER_DAY, onlineMinutes)
            }
            weeklyTrend.value = points
        }
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val activityRepository: ActivityRepository,
        private val balanceRepository: BalanceRepository,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(categoryRepository, activityRepository, balanceRepository, settingsStore) as T
    }
}
