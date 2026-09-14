package com.serkodesign.tepera.ui.diary

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MS_PER_DAY = 24 * 60 * 60 * 1000L

/** За прямим запитом користувача: перенесено зі StatsViewModel (`HistoryCard` жила на Stats) у
 * власний екран "Щоденник" на навбарі (node 2146:320, Figma) — сам розділ "сьогодні"+"вчора" не
 * змінився, лише переїхав. Тижневі підсумки розблокувань/медіани лишаються на Stats (вони
 * показувались НЕ в HistoryCard, а окремими рядками над графіками при period == WEEK). */
data class HistoryEntryItem(val entry: ActivityEntryEntity, val category: CategoryEntity)

data class HistoryDayGroup(val dayStartMillis: Long, val isToday: Boolean, val items: List<HistoryEntryItem>)

data class DiaryUiState(
    val history: List<HistoryDayGroup> = emptyList(),
    val unlockCountToday: Int? = null,
    val unlockCountYesterday: Int? = null,
    val lastPhoneUseYesterdayMillis: Long? = null
)

class DiaryViewModel(
    private val activityRepository: ActivityRepository,
    categoryRepository: CategoryRepository,
    private val balanceRepository: BalanceRepository,
    private val sleepWindowRepository: SleepWindowRepository,
    private val unlockRepository: UnlockRepository,
    private val pauseRepository: PauseRepository
) : ViewModel() {

    // Той самий спрощений принцип, що був у StatsViewModel: рахується один раз при створенні
    // ViewModel, не перераховується сам собою рівно опівночі, якщо екран лишається відкритим.
    private val historyRangeStart = startOfTodayMillis() - MS_PER_DAY

    private val unlockCounts = MutableStateFlow<Pair<Int?, Int?>>(null to null)
    private val lastPhoneUseYesterday = MutableStateFlow<Long?>(null)

    private val history: StateFlow<List<HistoryDayGroup>> = combine(
        activityRepository.observeEntriesInRange(historyRangeStart, Long.MAX_VALUE),
        categoryRepository.observeAllCategories()
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

    val uiState: StateFlow<DiaryUiState> = combine(history, unlockCounts, lastPhoneUseYesterday) { h, (today, yesterday), lastUse ->
        DiaryUiState(h, today, yesterday, lastUse)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    init {
        refresh()
    }

    /** Викликається при вході на екран і при поверненні з системних Налаштувань (та сама
     * причина, що BalanceViewModel/StatsViewModel — дозвіл на статистику надається поза
     * застосунком). */
    fun refresh() {
        viewModelScope.launch {
            if (!balanceRepository.hasUsageAccess()) {
                unlockCounts.value = null to null
                lastPhoneUseYesterday.value = null
                return@launch
            }
            val todayStart = startOfTodayMillis()
            val sleepWindows = sleepWindowRepository.getEnabledWindows()
            unlockCounts.value = if (unlockRepository.isSupported()) {
                unlockRepository.countUnlocks(todayStart, System.currentTimeMillis(), sleepWindows) to
                    unlockRepository.countUnlocks(todayStart - MS_PER_DAY, todayStart, sleepWindows)
            } else {
                null to null
            }
            lastPhoneUseYesterday.value =
                pauseRepository.lastPhoneUseForShiftedDay(System.currentTimeMillis(), daysBack = 0).lastUseMillis
        }
    }

    class Factory(
        private val activityRepository: ActivityRepository,
        private val categoryRepository: CategoryRepository,
        private val balanceRepository: BalanceRepository,
        private val sleepWindowRepository: SleepWindowRepository,
        private val unlockRepository: UnlockRepository,
        private val pauseRepository: PauseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DiaryViewModel(activityRepository, categoryRepository, balanceRepository, sleepWindowRepository, unlockRepository, pauseRepository) as T
    }
}
