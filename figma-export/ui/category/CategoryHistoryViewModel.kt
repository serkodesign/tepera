package com.serkodesign.tepera.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.util.localStartOfDay
import com.serkodesign.tepera.util.seriesRanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Тап по тілу картки категорії на Home (іконка/назва, не кнопки таймера/додавання часу, Figma
 * user-flow k6s4prQ9oK9x2uUvzHRghR node 14:791) відкриває повну історію записів САМЕ цієї
 * категорії. На відміну від "Щоденника" (DiaryViewModel), який свідомо обмежений сьогодні+вчора
 * одразу для всіх категорій (щоб не стати непридатно довгим), тут глибина необмежена — список
 * уже звужений однією категорією.
 */
data class CategoryHistoryDayGroup(val dayStartMillis: Long, val entries: List<ActivityEntryEntity>)

data class CategoryHistoryUiState(
    val category: CategoryEntity? = null,
    val groups: List<CategoryHistoryDayGroup> = emptyList(),
    /** Початок і кінець цілої багатодобової активності за її `seriesId` (для підпису в рядку). */
    val seriesRanges: Map<String, Pair<Long, Long>> = emptyMap()
)

class CategoryHistoryViewModel(
    private val categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    private val categoryId: String
) : ViewModel() {

    private val categoryState = MutableStateFlow<CategoryEntity?>(null)

    private val groups: StateFlow<List<CategoryHistoryDayGroup>> =
        activityRepository.observeEntriesInRange(0, Long.MAX_VALUE)
            .map { entries ->
                entries
                    .filter { it.categoryId == categoryId }
                    .groupBy { localStartOfDay(it.startTime) }
                    .map { (dayStart, dayEntries) ->
                        CategoryHistoryDayGroup(dayStart, dayEntries.sortedByDescending { it.startTime })
                    }
                    .sortedByDescending { it.dayStartMillis }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<CategoryHistoryUiState> = combine(categoryState, groups) { category, dayGroups ->
        CategoryHistoryUiState(category, dayGroups, seriesRanges(dayGroups.flatMap { it.entries }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryHistoryUiState())

    init {
        viewModelScope.launch { categoryState.value = categoryRepository.getById(categoryId) }
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val activityRepository: ActivityRepository,
        private val categoryId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CategoryHistoryViewModel(categoryRepository, activityRepository, categoryId) as T
    }
}
