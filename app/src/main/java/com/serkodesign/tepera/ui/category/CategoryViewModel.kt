package com.serkodesign.tepera.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CreateCategoryResult {
    data object Success : CreateCategoryResult()
    // FR-2.2: рівно одна кастомна категорія — і назавжди, бо архівація не звільняє слот
    // (жодного справжнього видалення з UI, FR-2.3).
    data object LimitReached : CreateCategoryResult()
}

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    val allCategories: StateFlow<List<CategoryEntity>> = repository.observeAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _createResult = MutableStateFlow<CreateCategoryResult?>(null)
    val createResult: StateFlow<CreateCategoryResult?> = _createResult.asStateFlow()

    fun archive(categoryId: String) {
        viewModelScope.launch { repository.archive(categoryId) }
    }

    fun unarchive(categoryId: String) {
        viewModelScope.launch { repository.unarchive(categoryId) }
    }

    fun createCustomCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            if (allCategories.value.any { it.isCustom }) {
                _createResult.value = CreateCategoryResult.LimitReached
                return@launch
            }
            repository.create(
                CategoryEntity(
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    isCustom = true,
                    sortOrder = allCategories.value.size
                )
            )
            _createResult.value = CreateCategoryResult.Success
        }
    }

    fun consumeCreateResult() {
        _createResult.value = null
    }

    class Factory(private val repository: CategoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CategoryViewModel(repository) as T
    }
}
