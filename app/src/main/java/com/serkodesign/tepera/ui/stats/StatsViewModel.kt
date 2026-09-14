package com.serkodesign.tepera.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

/**
 * T-14 (tepera-dev-spec.md): "доступне... в тижневому огляді — звичайним рядком, без
 * виділення" — НЕ на Home (де UnlockEstimateCard іде через власну картку "оцінка →
 * реальність"), а саме тут, на Stats, лише при period == WEEK. "Деталі дня" (сьогодні/вчора)
 * переїхали в `DiaryViewModel` разом з `HistoryCard` (за прямим запитом користувача, Щоденник —
 * нова вкладка навбару). `null` — нема доступу до статистики використання АБО API < 28
 * (`UnlockRepository.isSupported()` == false, документ: фіча приховується, не апроксимується).
 */
data class UnlockStatsUiState(val weekCount: Int? = null)

/**
 * Те саме, що [UnlockStatsUiState], для часу останнього використання (FR-D.7, T-10) — медіана
 * за 7 днів, той самий розрахунок, що другий рядок `LastPhoneUseEstimateCard`. "Вчора" (деталі
 * дня) — тепер у `DiaryViewModel`.
 */
data class LastPhoneUseStatsUiState(val weekMedianMillis: Long? = null)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val weeklyTrend: List<DailyBalancePoint> = emptyList(),
    val hasUsageAccess: Boolean = true,
    val unlockStats: UnlockStatsUiState = UnlockStatsUiState(),
    val lastPhoneUseStats: LastPhoneUseStatsUiState = LastPhoneUseStatsUiState()
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
    private val sleepWindowRepository: SleepWindowRepository,
    private val unlockRepository: UnlockRepository,
    private val pauseRepository: PauseRepository
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.WEEK)
    private val weeklyTrend = MutableStateFlow<List<DailyBalancePoint>>(emptyList())
    private val hasUsageAccess = MutableStateFlow(true)
    private val unlockStats = MutableStateFlow(UnlockStatsUiState())
    private val lastPhoneUseStats = MutableStateFlow(LastPhoneUseStatsUiState())

    // combine() підтримує щонайбільше 5 потоків з типізованою лямбдою (BalanceViewModel.kt —
    // той самий прийом) — усі три рахуються разом у refreshWeeklyTrend(), тож об'єднані в один
    // потік заздалегідь.
    private val trendAndUnlockStats = combine(weeklyTrend, unlockStats, lastPhoneUseStats) { trend, unlock, lastPhoneUse ->
        Triple(trend, unlock, lastPhoneUse)
    }

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
        trendAndUnlockStats,
        hasUsageAccess
    ) { p, breakdown, (trend, unlock, lastPhoneUse), access ->
        StatsUiState(p, breakdown, trend, access, unlock, lastPhoneUse)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    init {
        refreshWeeklyTrend()
    }

    // Довжина тренду Online-часу мала бути прив'язана до PeriodSelector так само, як
    // categoryBreakdown вище — до цього фіксу графік завжди показував ті самі 7 днів під
    // заголовком "Тижневий тренд" незалежно від обраного Дня/Тижня/Місяця (знайдено живим
    // тестуванням: перемикання на "Місяць" не міняло ні дані, ні підпис графіка).
    private fun daysForPeriod(p: StatsPeriod): Int = when (p) {
        StatsPeriod.DAY -> 1
        StatsPeriod.WEEK -> 7
        StatsPeriod.MONTH -> 30
    }

    private suspend fun computeTrend(p: StatsPeriod): List<DailyBalancePoint> {
        val todayStart = startOfTodayMillis()
        // FR-3.5 (SRS v2.5): та сама точка старту дня, що на Home (BalanceViewModel.refresh())
        // — перше суттєве розблокування після вікна сну, не локальна північ. Рахується один
        // раз тут, бо стосується лише сьогоднішньої (daysAgo == 0) точки тренду.
        val sleepWindows = sleepWindowRepository.getEnabledWindows()
        val todayDayStart = balanceRepository.calculateDayStartMillis(sleepWindows)
        return (daysForPeriod(p) - 1 downTo 0).map { daysAgo ->
            // Для сьогодні (daysAgo == 0) день ще не завершився — межа старту та сама точка
            // старту дня, що на Home, не північ. Для минулих завершених діб — календарна доба
            // [північ, наступна північ). Без ділення на знаменник (FR-P.6 вище) — просто
            // абсолютні хвилини Online за цю добу.
            val dayStart = if (daysAgo == 0) todayDayStart else todayStart - daysAgo * MS_PER_DAY
            val dayEnd = if (daysAgo == 0) System.currentTimeMillis() else dayStart + MS_PER_DAY
            val onlineMinutes = balanceRepository.getOnlineMinutes(dayStart, dayEnd)
            // Точка на графіку лишається прив'язана до календарного дня (todayStart - daysAgo
            // * MS_PER_DAY), не до фактичної точки старту дня — інакше вісь X тренду
            // сьогоднішньої точки зсувалась би вбік від решти днів.
            DailyBalancePoint(todayStart - daysAgo * MS_PER_DAY, onlineMinutes)
        }
    }

    fun selectPeriod(p: StatsPeriod) {
        period.value = p
        if (hasUsageAccess.value) {
            viewModelScope.launch { weeklyTrend.value = computeTrend(p) }
        }
    }

    /** Викликається при вході на екран і при поверненні з системних Налаштувань. */
    fun refreshWeeklyTrend() {
        viewModelScope.launch {
            val access = balanceRepository.hasUsageAccess()
            hasUsageAccess.value = access
            if (!access) {
                weeklyTrend.value = emptyList()
                unlockStats.value = UnlockStatsUiState()
                lastPhoneUseStats.value = LastPhoneUseStatsUiState()
                return@launch
            }
            weeklyTrend.value = computeTrend(period.value)

            // T-14: "тижневий огляд" — звичайним рядком, не на Home. API < 28 →
            // isSupported() == false → лишається UnlockStatsUiState() (null).
            unlockStats.value = if (unlockRepository.isSupported()) {
                val sleepWindows = sleepWindowRepository.getEnabledWindows()
                UnlockStatsUiState(
                    weekCount = unlockRepository.countUnlocks(
                        startOfTodayMillis() - 6 * MS_PER_DAY, System.currentTimeMillis(), sleepWindows
                    )
                )
            } else {
                UnlockStatsUiState()
            }

            // T-10: "тижневий огляд" — медіана за 7 днів, той самий розрахунок, що другий рядок
            // LastPhoneUseEstimateCard.
            lastPhoneUseStats.value = LastPhoneUseStatsUiState(
                weekMedianMillis = pauseRepository.medianLastPhoneUseMillis(System.currentTimeMillis())
            )
        }
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val activityRepository: ActivityRepository,
        private val balanceRepository: BalanceRepository,
        private val sleepWindowRepository: SleepWindowRepository,
        private val unlockRepository: UnlockRepository,
        private val pauseRepository: PauseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(categoryRepository, activityRepository, balanceRepository, sleepWindowRepository, unlockRepository, pauseRepository) as T
    }
}
