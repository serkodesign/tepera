package com.serkodesign.tepera.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
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

/** Історія на Stats (за прямим запитом користувача, не в SRS) — один запис + резолвлена категорія. */
data class HistoryEntryItem(val entry: ActivityEntryEntity, val category: CategoryEntity)

/** Один день історії — [dayStartMillis] лишається саме календарною північчю (не точкою старту
 * дня), бо угруповання "сьогодні"/"вчора" тут про календарний день, у який записана активність. */
data class HistoryDayGroup(val dayStartMillis: Long, val isToday: Boolean, val items: List<HistoryEntryItem>)

/**
 * T-14 (tepera-dev-spec.md): "доступне в деталях дня і в тижневому огляді — звичайним рядком,
 * без виділення" — НЕ на Home (де UnlockEstimateCard і йде через власну картку "оцінка →
 * реальність"), а саме тут, на Stats. `null` — нема доступу до статистики використання АБО
 * API < 28 (`UnlockRepository.isSupported()` == false, документ: фіча приховується, не
 * апроксимується); поле відсутнє в UI, а не показане як "0" (0 читалось би як підтверджений факт,
 * не "нема даних").
 */
data class UnlockStatsUiState(
    val todayCount: Int? = null,
    val yesterdayCount: Int? = null,
    val weekCount: Int? = null
)

/**
 * T-10 (tepera-dev-spec.md): те саме "доступне в деталях дня і в тижневому огляді", що
 * [UnlockStatsUiState], для часу останнього використання (FR-D.7). Лише "вчора" (не "сьогодні" —
 * доба, що ще триває, не має завершеного, добре визначеного "останнього" використання, той самий
 * принцип, що обґрунтовує "метрику вчорашнього дня" в самій картці-оцінці). "Тижневий огляд" —
 * медіана за 7 днів, той самий розрахунок, що другий рядок картки (`PauseRepository.
 * medianLastPhoneUseMillis()`). `null` полів — нема доступу/даних, рядок відсутній, не "00:00".
 */
data class LastPhoneUseStatsUiState(
    val yesterdayMillis: Long? = null,
    val weekMedianMillis: Long? = null
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val weeklyTrend: List<DailyBalancePoint> = emptyList(),
    val hasUsageAccess: Boolean = true,
    val history: List<HistoryDayGroup> = emptyList(),
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

    // Історія (за прямим запитом користувача, не в SRS): "сьогодні" + "вчора", НЕ прив'язана до
    // period/PeriodSelector вище — редагування/видалення тижня чи місяця записів одразу створило
    // б непридатно довгий список, тому завжди рівно ці два дні, як у референсному макеті.
    // `historyRangeStart` рахується один раз при створенні ViewModel (та сама спрощена
    // передумова "екран живе недовго", що й у решті застосунку) — якщо екран лишається відкритим
    // рівно через північ, межа "вчора" не зсунеться сама, доки Stats не перевідкриють.
    private val historyRangeStart = startOfTodayMillis() - MS_PER_DAY

    private val history: StateFlow<List<HistoryDayGroup>> = combine(
        activityRepository.observeEntriesInRange(historyRangeStart, Long.MAX_VALUE),
        categoryRepository.observeAllCategories() // усі, не лише активні — стара запись архівованої категорії й далі має ім'я/іконку
    ) { entries, categories ->
        val categoryById = categories.associateBy { it.id }
        val todayStart = startOfTodayMillis()
        val (todayEntries, yesterdayEntries) = entries.partition { it.startTime >= todayStart }
        fun toGroup(dayStart: Long, isToday: Boolean, dayEntries: List<ActivityEntryEntity>) =
            HistoryDayGroup(
                dayStartMillis = dayStart,
                isToday = isToday,
                items = dayEntries.mapNotNull { e -> categoryById[e.categoryId]?.let { HistoryEntryItem(e, it) } }
            ).takeIf { it.items.isNotEmpty() }
        listOfNotNull(
            toGroup(todayStart, true, todayEntries),
            toGroup(historyRangeStart, false, yesterdayEntries)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<StatsUiState> = combine(
        period,
        categoryBreakdown,
        trendAndUnlockStats,
        hasUsageAccess,
        history
    ) { p, breakdown, (trend, unlock, lastPhoneUse), access, historyGroups ->
        StatsUiState(p, breakdown, trend, access, historyGroups, unlock, lastPhoneUse)
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
                unlockStats.value = UnlockStatsUiState()
                lastPhoneUseStats.value = LastPhoneUseStatsUiState()
                return@launch
            }
            val todayStart = startOfTodayMillis()
            // FR-3.5 (SRS v2.5): та сама точка старту дня, що на Home (BalanceViewModel.refresh())
            // — перше суттєве розблокування після вікна сну, не локальна північ. Рахується один
            // раз тут, бо стосується лише сьогоднішньої (daysAgo == 0) точки тренду.
            val sleepWindows = sleepWindowRepository.getEnabledWindows()
            val todayDayStart = balanceRepository.calculateDayStartMillis(sleepWindows)
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

            // T-14: "деталі дня" (сьогодні/вчора) і "тижневий огляд" — звичайним рядком, не на
            // Home. API < 28 → isSupported() == false → лишається UnlockStatsUiState() (усі null).
            unlockStats.value = if (unlockRepository.isSupported()) {
                UnlockStatsUiState(
                    todayCount = unlockRepository.countUnlocks(todayStart, System.currentTimeMillis(), sleepWindows),
                    yesterdayCount = unlockRepository.countUnlocks(todayStart - MS_PER_DAY, todayStart, sleepWindows),
                    weekCount = unlockRepository.countUnlocks(todayStart - 6 * MS_PER_DAY, System.currentTimeMillis(), sleepWindows)
                )
            } else {
                UnlockStatsUiState()
            }

            // T-10: "деталі дня" (лише вчора — сьогодні ще не має завершеного останнього
            // використання) і "тижневий огляд" (медіана за 7 днів, той самий розрахунок, що
            // другий рядок LastPhoneUseEstimateCard).
            val now = System.currentTimeMillis()
            lastPhoneUseStats.value = LastPhoneUseStatsUiState(
                yesterdayMillis = pauseRepository.lastPhoneUseForShiftedDay(now, daysBack = 0).lastUseMillis,
                weekMedianMillis = pauseRepository.medianLastPhoneUseMillis(now)
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
