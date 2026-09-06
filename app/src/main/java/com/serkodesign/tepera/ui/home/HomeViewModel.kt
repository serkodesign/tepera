package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoryTodaySummary(val category: CategoryEntity, val minutesToday: Int)

class HomeViewModel(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository
) : ViewModel() {

    // "to" = Long.MAX_VALUE навмисно: діапазон лише відсікає записи ДО півночі, а не querить
    // заново з новим "зараз" — інакше запис, доданий пізніше в тому самому сеансі Home-екрана,
    // міг би випасти з фіксованого на момент ініціалізації верхнього кордону.
    val todaySummary: StateFlow<List<CategoryTodaySummary>> = combine(
        categoryRepository.observeActiveCategories(),
        activityRepository.observeEntriesInRange(startOfTodayMillis(), Long.MAX_VALUE)
    ) { categories, entries ->
        categories.sortedBy { it.sortOrder }.map { category ->
            val minutes = entries.filter { it.categoryId == category.id }.sumOf { it.durationMinutes }
            CategoryTodaySummary(category, minutes)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val activityRepository: ActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(categoryRepository, activityRepository) as T
    }
}
