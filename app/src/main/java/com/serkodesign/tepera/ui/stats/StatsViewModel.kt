package com.serkodesign.tepera.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.GapDetectionConfig
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.util.offlineUnloggedMinutes
import com.serkodesign.tepera.util.startOfTodayMillis
import com.serkodesign.tepera.widget.DAILY_GRID_SLOT_COUNT
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

/**
 * FR-5.2: період для стовпчикової діаграми розподілу офлайн-часу по категоріях. **DAY — це ВЧОРА**
 * (завершена календарна доба, за прямим запитом користувача), а не сьогодні: сьогодні вже є на Home
 * і в Щоденнику, а вчорашня доба — повна й дає змістовну хронологію, паузи та порівняння.
 */
enum class StatsPeriod { DAY, WEEK }

/**
 * Скільки днів охоплює кожен період — 1/7/30, ковзне вікно (не календарний тиждень/місяць,
 * MVP-спрощення). Публічна, бо за прямим запитом користувача той самий перемикач тепер керує
 * й тепловим патерном доби (`StatsScreen` передає це значення в `PatternViewModel.refresh()`).
 */
fun daysForStatsPeriod(p: StatsPeriod): Int = when (p) {
    StatsPeriod.DAY -> 1
    StatsPeriod.WEEK -> 7
}

/**
 * [minutes] — сума ЦІЛИХ хвилин записів категорії за період; [subSeconds] — накопичені секунди коротких
 * таймерів і "хвостів" (див. SubMinuteStore), які враховуються в даних, але не складали цілу хвилину.
 */
data class CategoryBreakdownItem(val category: CategoryEntity, val minutes: Int, val subSeconds: Int = 0) {
    val totalSeconds: Int get() = minutes * 60 + subSeconds
}

/** Категорії з сумою за період менше за цей поріг у графіку показуються як "<5 хв", а не точними хвилинами. */
const val MIN_SHOWN_CATEGORY_SECONDS = 5 * 60

/**
 * FR-5.3: одна точка тижневого тренду — доба + абсолютні Online-хвилини на неї.
 * FR-P.6 (SRS v2.5): свідомо НЕ частка/відсоток від знаменника Grace Period Buffer — голий %
 * без контексту читається як оцінка, а не факт. Абсолютний час порівнюється сам із собою день
 * до дня, без прихованого "буфера справедливості", який мав сенс лише для Home-шкали сьогодні.
 */
data class DailyBalancePoint(
    val dayStartMillis: Long,
    /** Online за ВСЮ добу (на графіку й у рядку "Online") — включно з нічним використанням телефону. */
    val onlineMinutes: Int,
    /** Довжина неспаної доби (від старту дня, без вікон сну). */
    val dayLengthMinutes: Int = 0,
    /**
     * "Офлайн без запису" за об'єднанням: неспана доба мінус час, зайнятий Online АБО записом (перекриття
     * рахується один раз, див. [offlineUnloggedMinutes]).
     */
    val offlineMinutes: Int = 0
)

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
    val period: StatsPeriod = StatsPeriod.DAY,
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val weeklyTrend: List<DailyBalancePoint> = emptyList(),
    val hasUsageAccess: Boolean = true,
    val unlockStats: UnlockStatsUiState = UnlockStatsUiState(),
    val lastPhoneUseStats: LastPhoneUseStatsUiState = LastPhoneUseStatsUiState(),
    /** Деталі вчорашньої доби — лише для period == DAY. */
    val dayDetails: DayDetailsUiState = DayDetailsUiState()
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
    private val pauseRepository: PauseRepository,
    private val settingsStore: SettingsStore,
    private val subMinuteStore: com.serkodesign.tepera.data.local.SubMinuteStore
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.DAY)
    private val weeklyTrend = MutableStateFlow<List<DailyBalancePoint>>(emptyList())
    private val hasUsageAccess = MutableStateFlow(true)
    private val unlockStats = MutableStateFlow(UnlockStatsUiState())
    private val lastPhoneUseStats = MutableStateFlow(LastPhoneUseStatsUiState())
    private val dayDetails = MutableStateFlow(DayDetailsUiState())

    // combine() підтримує щонайбільше 5 потоків з типізованою лямбдою (BalanceViewModel.kt —
    // той самий прийом) — усі три рахуються разом у refreshWeeklyTrend(), тож об'єднані в один
    // потік заздалегідь.
    private val trendAndUnlockStats = combine(weeklyTrend, unlockStats, lastPhoneUseStats) { trend, unlock, lastPhoneUse ->
        Triple(trend, unlock, lastPhoneUse)
    }

    // "Тиждень"/"Місяць" — ковзне вікно останніх 7/30 днів, не календарний тиждень/місяць
    // (спрощення MVP, узгоджується з відомим timezone/календарним обмеженням SRS розділ 11).
    // DAY — вчорашня календарна доба [північ, північ), верхня межа -1 мс, бо запит BETWEEN inclusive.
    private fun periodRange(p: StatsPeriod): Pair<Long, Long> = when (p) {
        StatsPeriod.DAY -> (startOfTodayMillis() - MS_PER_DAY) to (startOfTodayMillis() - 1)
        StatsPeriod.WEEK -> (startOfTodayMillis() - 6 * MS_PER_DAY) to Long.MAX_VALUE
    }

    private val categoryBreakdown: StateFlow<List<CategoryBreakdownItem>> = period
        .flatMapLatest { p ->
            val (from, to) = periodRange(p)
            combine(
                categoryRepository.observeActiveCategories(),
                activityRepository.observeEntriesInRange(from, to),
                subMinuteStore.observeSecondsByCategory(from, minOf(to, System.currentTimeMillis()))
            ) { categories, entries, subSeconds ->
                categories.sortedBy { it.sortOrder }.map { category ->
                    val minutes = entries.filter { it.categoryId == category.id }.sumOf { it.durationMinutes }
                    CategoryBreakdownItem(category, minutes, subSeconds[category.id] ?: 0)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<StatsUiState> = combine(
        period,
        categoryBreakdown,
        trendAndUnlockStats,
        hasUsageAccess,
        dayDetails
    ) { p, breakdown, (trend, unlock, lastPhoneUse), access, day ->
        StatsUiState(p, breakdown, trend, access, unlock, lastPhoneUse, day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    init {
        refreshWeeklyTrend()
    }

    // Довжина тренду Online-часу мала бути прив'язана до PeriodSelector так само, як
    // categoryBreakdown вище — до цього фіксу графік завжди показував ті самі 7 днів під
    // заголовком "Тижневий тренд" незалежно від обраного Дня/Тижня/Місяця (знайдено живим
    // тестуванням: перемикання на "Місяць" не міняло ні дані, ні підпис графіка).
    private fun daysForPeriod(p: StatsPeriod): Int = daysForStatsPeriod(p)

    private suspend fun computeTrend(p: StatsPeriod): List<DailyBalancePoint> {
        val todayStart = startOfTodayMillis()
        // FR-3.5 (SRS v2.5): та сама точка старту дня, що на Home (BalanceViewModel.refresh())
        // — перше суттєве розблокування після вікна сну, не локальна північ. Рахується один
        // раз тут, бо стосується лише сьогоднішньої (daysAgo == 0) точки тренду.
        val sleepWindows = sleepWindowRepository.getEnabledWindows()
        val todayDayStart = balanceRepository.calculateDayStartMillis(sleepWindows)
        // DAY — єдина точка: вчорашня завершена доба (daysAgo == 1); WEEK — 7 діб, включно із сьогодні.
        val offsets = if (p == StatsPeriod.DAY) listOf(1) else (daysForPeriod(p) - 1 downTo 0).toList()
        val periodEntries = activityRepository
            .observeEntriesInRange(todayStart - offsets.max() * MS_PER_DAY, Long.MAX_VALUE).first()
        val now = System.currentTimeMillis()
        return offsets.map { daysAgo ->
            // Сьогодні (daysAgo == 0) — Online від точки старту дня до "зараз", як на Home. Минулі доби: Online
            // за ВСЮ календарну добу (нічне використання телефону — теж Online, його не ховаємо). "Офлайн" — окремо:
            // неспана доба (від старту дня, без вікон сну T-12, той самий інструмент, що на Home) мінус ОБ'ЄДНАННЯ
            // Online й записів, тож перекриття не віднімається двічі. Точка на графіку лишається прив'язана до
            // календарного дня, не до точки старту дня — інакше вісь X зсувалась би вбік. Без ділення на
            // знаменник (FR-P.6) — просто абсолютні хвилини.
            val midnight = todayStart - daysAgo * MS_PER_DAY
            val windowStart = if (daysAgo == 0) todayDayStart else balanceRepository.calculateDayStartMillis(sleepWindows, midnight, midnight + MS_PER_DAY)
            val windowEnd = if (daysAgo == 0) now else midnight + MS_PER_DAY
            val online = if (daysAgo == 0) {
                balanceRepository.getOnlineMinutes(todayDayStart, now)
            } else {
                balanceRepository.getOnlineMinutes(midnight, midnight + MS_PER_DAY)
            }
            if (windowStart >= windowEnd) {
                // Суттєвого розблокування в добі не було — "неспаної" частини нема.
                return@map DailyBalancePoint(midnight, online, 0, 0)
            }
            val onlineSpans = balanceRepository.getOnlineIntervals(windowStart, windowEnd)
            val dayEntries = periodEntries.filter { it.startTime < windowEnd && it.startTime + it.durationMinutes * 60_000L > windowStart }
            DailyBalancePoint(
                midnight, online,
                balanceRepository.calculateDayLengthMinutes(windowStart, sleepWindows, windowEnd),
                offlineUnloggedMinutes(windowStart, windowEnd, onlineSpans, dayEntries, sleepWindows)
            )
        }
    }

    fun selectPeriod(p: StatsPeriod) {
        period.value = p
        viewModelScope.launch {
            if (hasUsageAccess.value) weeklyTrend.value = computeTrend(p)
            if (p == StatsPeriod.DAY) dayDetails.value = computeDayDetails(hasUsageAccess.value)
        }
    }

    /**
     * Деталі ВЧОРАШНЬОЇ доби (Статистика → День): хронологія (записи + Online + паузи), паузи,
     * порівняння Online із власною типовою добою, межі дня й розблокування. Усе — факти без оцінок;
     * розблокування й "востаннє брав телефон" — саме "деталі дня" на Stats, де їх дозволяє документ
     * (T-14/T-10), не на Home. [access] = false — лише те, що є без статистики використання
     * (записи вручну), решта лишається порожньою.
     */
    private suspend fun computeDayDetails(access: Boolean): DayDetailsUiState {
        val todayStart = startOfTodayMillis()
        val yesterdayStart = todayStart - MS_PER_DAY
        val entries = activityRepository.observeEntriesInRange(yesterdayStart, todayStart - 1).first()
        val categoriesById = categoryRepository.observeAllCategories().first().associateBy { it.id }
        if (!access) {
            return DayDetailsUiState(
                dayStartMillis = yesterdayStart,
                timeline = buildDayTimeline(yesterdayStart, null, entries, IntArray(DAILY_GRID_SLOT_COUNT), emptyList(), onlineKnown = false),
                categoriesById = categoriesById
            )
        }

        val sleepWindows = sleepWindowRepository.getEnabledWindows()
        val config = GapDetectionConfig.forSensitivity(settingsStore.gapSensitivity.first())
        val dayStart = balanceRepository.calculateDayStartMillis(sleepWindows, yesterdayStart, todayStart)
        // calculateDayStartMillis повертає searchEnd, коли суттєвого розблокування не було — тоді дня "не було".
        val firstUse = dayStart.takeIf { it < todayStart }
        val online = balanceRepository.getOnlineMinutesPerSlot(yesterdayStart, 30, DAILY_GRID_SLOT_COUNT)
        val scan = pauseRepository.scan(firstUse ?: yesterdayStart, todayStart, sleepWindows, config)

        val longest = scan.gaps.maxByOrNull { it.durationMinutes }
        val pauses = longest?.let { gap ->
            val gapEnd = gap.startTime + gap.durationMinutes * 60_000L
            // Позначена пауза = запис GAP_LABELED усередині проміжку; його категорія й названа в підсумку.
            val labeled = entries.firstOrNull {
                it.source == EntrySource.GAP_LABELED && it.startTime >= gap.startTime && it.startTime < gapEnd
            }
            PauseSummary(scan.gaps.size, gap, labeled?.let { categoriesById[it.categoryId] })
        }

        // Типовий день: Online за 7 діб ДО вчорашньої (2..8), лише ті, що повністю в системній історії.
        val earliest = balanceRepository.earliestUsageEventMillis(todayStart - 9 * MS_PER_DAY)
        val comparison = if (earliest != null && yesterdayStart >= earliest) {
            val previous = (2..8).map { todayStart - it * MS_PER_DAY }
                .filter { it >= earliest }
                .map { balanceRepository.getOnlineMinutes(it, it + MS_PER_DAY) }
            val typical = if (previous.size >= MIN_DAYS_FOR_TYPICAL) medianOf(previous) else null
            typical?.let {
                OnlineComparison(balanceRepository.getOnlineMinutes(yesterdayStart, todayStart), it, previous.size)
            }
        } else {
            null
        }

        return DayDetailsUiState(
            dayStartMillis = yesterdayStart,
            timeline = buildDayTimeline(yesterdayStart, firstUse, entries, online, scan.gaps, sleepWindows = sleepWindows),
            categoriesById = categoriesById,
            pauses = pauses,
            comparison = comparison,
            firstUseMillis = firstUse,
            lastUseMillis = pauseRepository.lastPhoneUseForShiftedDay(System.currentTimeMillis(), daysBack = 0).lastUseMillis,
            unlockCount = if (unlockRepository.isSupported()) {
                unlockRepository.countUnlocks(yesterdayStart, todayStart, sleepWindows)
            } else {
                null
            }
        )
    }

    /** Викликається при вході на екран і при поверненні з системних Налаштувань. */
    fun refreshWeeklyTrend() {
        viewModelScope.launch {
            val access = balanceRepository.hasUsageAccess()
            hasUsageAccess.value = access
            if (!access) {
                if (period.value == StatsPeriod.DAY) dayDetails.value = computeDayDetails(false)
                weeklyTrend.value = emptyList()
                unlockStats.value = UnlockStatsUiState()
                lastPhoneUseStats.value = LastPhoneUseStatsUiState()
                return@launch
            }
            weeklyTrend.value = computeTrend(period.value)
            if (period.value == StatsPeriod.DAY) dayDetails.value = computeDayDetails(true)

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
        private val pauseRepository: PauseRepository,
        private val settingsStore: SettingsStore,
        private val subMinuteStore: com.serkodesign.tepera.data.local.SubMinuteStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(categoryRepository, activityRepository, balanceRepository, sleepWindowRepository, unlockRepository, pauseRepository, settingsStore, subMinuteStore) as T
    }
}
