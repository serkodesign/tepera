package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CategoryTodaySummary(
    val category: CategoryEntity,
    val minutesToday: Int,
    val trackingStartTime: Long? = null // не-null, поки для цієї категорії йде живий таймер
)

/**
 * Тап по категорії на Home: перший тап починає живий таймер (FR-1.x — швидка альтернатива
 * Add Entry), повторний тап по ТІЙ САМІЙ категорії зупиняє його й зберігає запис. Кілька
 * категорій можуть таймитись одночасно — узгоджується з FR-1.3 (перекриття лише в межах
 * однієї категорії, різні категорії законно перекриваються, CLAUDE.md).
 */
class HomeViewModel(
    categoryRepository: CategoryRepository,
    private val activityRepository: ActivityRepository,
    private val activeTimerStore: ActiveTimerStore
) : ViewModel() {

    // "to" = Long.MAX_VALUE навмисно: діапазон лише відсікає записи ДО півночі, а не querить
    // заново з новим "зараз" — інакше запис, доданий пізніше в тому самому сеансі Home-екрана,
    // міг би випасти з фіксованого на момент ініціалізації верхнього кордону.
    val todaySummary: StateFlow<List<CategoryTodaySummary>> = combine(
        categoryRepository.observeActiveCategories(),
        activityRepository.observeEntriesInRange(startOfTodayMillis(), Long.MAX_VALUE),
        activeTimerStore.activeTimers
    ) { categories, entries, activeTimers ->
        categories.sortedBy { it.sortOrder }.map { category ->
            val minutes = entries.filter { it.categoryId == category.id }.sumOf { it.durationMinutes }
            CategoryTodaySummary(category, minutes, activeTimers[category.id])
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Перемикач: почати таймер, якщо для цієї категорії він не йде, інакше зупинити й зберегти. */
    fun toggleTimer(categoryId: String) {
        viewModelScope.launch {
            val startTime = activeTimerStore.stop(categoryId)
            if (startTime == null) {
                activeTimerStore.start(categoryId)
            } else {
                val minutes = ((System.currentTimeMillis() - startTime) / 60_000.0)
                    .roundToInt()
                    .coerceAtLeast(1) // навіть кількасекундний тап логує хоч 1 хв, а не 0
                // forceOverwrite: зупинка живого таймера — швидка дія без діалогів; overlap-перевірка
                // (FR-1.4) створена для ручного вводу, тут би лише заважала непередбачувано.
                activityRepository.addEntry(
                    ActivityEntryEntity(
                        categoryId = categoryId,
                        startTime = startTime,
                        durationMinutes = minutes
                    ),
                    forceOverwrite = true
                )
            }
        }
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val activityRepository: ActivityRepository,
        private val activeTimerStore: ActiveTimerStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(categoryRepository, activityRepository, activeTimerStore) as T
    }
}
