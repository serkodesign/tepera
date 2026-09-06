package com.serkodesign.tepera.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.SaveEntryResult
import com.serkodesign.tepera.util.currentMinuteOfDay
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DurationMode { PRESETS, MANUAL, INTERVAL }

private const val MAX_NOTE_LENGTH = 250

/**
 * FR-1.1/1.2: durationMinutes резолвиться з активного режиму вводу — пресети (стек +15/+30/
 * +60/+120), ручний ввід або інтервал (кінець - початок).
 */
data class AddEntryUiState(
    val selectedCategoryId: String? = null, // FR-4.1: може прийти передвибраним з кнопки віджета
    val mode: DurationMode = DurationMode.PRESETS,
    val presetMinutes: Int = 0,
    val manualMinutesText: String = "",
    val dateMillis: Long = startOfTodayMillis(),
    val startMinuteOfDay: Int = currentMinuteOfDay(),
    val endMinuteOfDay: Int = currentMinuteOfDay(),
    val note: String = "",
    val categoryRequiredError: Boolean = false,
    val intervalInvalidError: Boolean = false,
    val overlapEntries: List<ActivityEntryEntity>? = null,
    val saved: Boolean = false
) {
    val durationMinutes: Int
        get() = when (mode) {
            DurationMode.PRESETS -> presetMinutes
            DurationMode.MANUAL -> manualMinutesText.toIntOrNull() ?: 0
            DurationMode.INTERVAL -> endMinuteOfDay - startMinuteOfDay
        }
}

class AddEntryViewModel(
    categoryRepository: CategoryRepository,
    private val activityRepository: ActivityRepository,
    initialCategoryId: String? = null
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(AddEntryUiState(selectedCategoryId = initialCategoryId))
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, categoryRequiredError = false)
    }

    fun selectMode(mode: DurationMode) {
        _uiState.value = _uiState.value.copy(mode = mode, intervalInvalidError = false)
    }

    fun addPresetMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(presetMinutes = _uiState.value.presetMinutes + minutes)
    }

    fun resetPresetMinutes() {
        _uiState.value = _uiState.value.copy(presetMinutes = 0)
    }

    fun setManualMinutes(text: String) {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        _uiState.value = _uiState.value.copy(manualMinutesText = digitsOnly)
    }

    fun setDate(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(dateMillis = dateMillis)
    }

    fun setStartMinuteOfDay(minute: Int) {
        _uiState.value = _uiState.value.copy(startMinuteOfDay = minute, intervalInvalidError = false)
    }

    fun setEndMinuteOfDay(minute: Int) {
        _uiState.value = _uiState.value.copy(endMinuteOfDay = minute, intervalInvalidError = false)
    }

    fun setNote(text: String) {
        _uiState.value = _uiState.value.copy(note = text.take(MAX_NOTE_LENGTH))
    }

    fun dismissOverlapDialog() {
        _uiState.value = _uiState.value.copy(overlapEntries = null)
    }

    fun save(forceOverwrite: Boolean = false) {
        val state = _uiState.value

        val categoryId = state.selectedCategoryId
        if (categoryId == null) {
            _uiState.value = state.copy(categoryRequiredError = true)
            return
        }
        if (state.mode == DurationMode.INTERVAL && state.durationMinutes <= 0) {
            _uiState.value = state.copy(intervalInvalidError = true)
            return
        }

        val duration = state.durationMinutes.coerceAtLeast(1)
        val startTime = state.dateMillis + state.startMinuteOfDay * 60_000L

        viewModelScope.launch {
            when (
                val result = activityRepository.addEntry(
                    ActivityEntryEntity(
                        categoryId = categoryId,
                        startTime = startTime,
                        durationMinutes = duration,
                        note = state.note.ifBlank { null }
                    ),
                    forceOverwrite = forceOverwrite
                )
            ) {
                is SaveEntryResult.Success ->
                    _uiState.value = _uiState.value.copy(saved = true, overlapEntries = null)
                is SaveEntryResult.OverlapDetected ->
                    _uiState.value = _uiState.value.copy(overlapEntries = result.existing)
            }
        }
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val activityRepository: ActivityRepository,
        private val initialCategoryId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddEntryViewModel(categoryRepository, activityRepository, initialCategoryId) as T
    }
}
