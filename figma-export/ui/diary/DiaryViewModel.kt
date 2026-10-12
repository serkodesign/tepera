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
import com.serkodesign.tepera.util.localStartOfDay
import com.serkodesign.tepera.util.startOfTodayMillis
import com.serkodesign.tepera.util.seriesRanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private const val MS_PER_DAY = 24 * 60 * 60 * 1000L

/** Скільки діб (разом із сьогоднішньою) показує Щоденник — за прямим запитом користувача, 2 тижні.
 * Самі записи в БД не видаляються ніколи; це лише глибина показу. */
const val DIARY_HISTORY_DAYS = 14

/** За прямим запитом користувача: перенесено зі StatsViewModel (`HistoryCard` жила на Stats) у
 * власний екран "Щоденник" на навбарі (node 2146:320, Figma). Спершу показував лише "сьогодні" і
 * "вчора", тепер — [DIARY_HISTORY_DAYS] діб. Тижневі підсумки розблокувань/медіани лишаються на
 * Stats (вони показувались НЕ в HistoryCard, а окремими рядками над графіками при period == WEEK). */
data class HistoryEntryItem(
    val entry: ActivityEntryEntity,
    val category: CategoryEntity,
    /** Початок і кінець ЦІЛОЇ активності, якщо запис — частина багатодобової (див. `splitAtDayRollover`). */
    val seriesRange: Pair<Long, Long>? = null
)

/** [daysAgo]: 0 — сьогодні, 1 — вчора, далі — старші доби. */
data class HistoryDayGroup(val dayStartMillis: Long, val daysAgo: Int, val items: List<HistoryEntryItem>)

data class DiaryUiState(
    val history: List<HistoryDayGroup> = emptyList(),
    /** Початок доби -> розблокування; доби без надійних даних (старші за системну історію) відсутні. */
    val unlockCountsByDay: Map<Long, Int> = emptyMap(),
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
    private val historyRangeStart = startOfDayDaysAgo(DIARY_HISTORY_DAYS - 1)

    private val unlockCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val lastPhoneUseYesterday = MutableStateFlow<Long?>(null)

    private val history: StateFlow<List<HistoryDayGroup>> = combine(
        activityRepository.observeEntriesInRange(historyRangeStart, Long.MAX_VALUE),
        categoryRepository.observeAllCategories()
    ) { entries, categories ->
        val categoryById = categories.associateBy { it.id }
        val seriesRangeById = seriesRanges(entries)
        val todayStart = startOfTodayMillis()
        entries.groupBy { localStartOfDay(it.startTime) }
            .toSortedMap(compareByDescending { it })
            .mapNotNull { (dayStart, dayEntries) ->
                val items = dayEntries.mapNotNull { e -> categoryById[e.categoryId]?.let { HistoryEntryItem(e, it, e.seriesId?.let(seriesRangeById::get)) } }
                if (items.isEmpty()) return@mapNotNull null
                // round, не floor: перехід на літній/зимовий час робить добу 23 або 25 год.
                val daysAgo = Math.round((todayStart - dayStart).toDouble() / MS_PER_DAY).toInt()
                HistoryDayGroup(dayStart, daysAgo, items)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<DiaryUiState> = combine(history, unlockCounts, lastPhoneUseYesterday) { h, counts, lastUse ->
        DiaryUiState(h, counts, lastUse)
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
                unlockCounts.value = emptyMap()
                lastPhoneUseYesterday.value = null
                return@launch
            }
            val sleepWindows = sleepWindowRepository.getEnabledWindows()
            unlockCounts.value = unlockRepository.countUnlocksByDay(
                from = historyRangeStart,
                to = System.currentTimeMillis(),
                sleepWindows = sleepWindows
            )
            lastPhoneUseYesterday.value =
                pauseRepository.lastPhoneUseForShiftedDay(System.currentTimeMillis(), daysBack = 0).lastUseMillis
        }
    }

    private fun startOfDayDaysAgo(days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = startOfTodayMillis()
            add(Calendar.DAY_OF_YEAR, -days)
        }.timeInMillis

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
