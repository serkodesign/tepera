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
import com.serkodesign.tepera.util.localStartOfDay
import com.serkodesign.tepera.util.minuteOfDay
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_NOTE_LENGTH = 250
private const val DEFAULT_INTERVAL_MINUTES = 30
private const val MINUTES_IN_DAY = 24 * 60

/**
 * Час запису вводиться ЛИШЕ інтервалом (початок → кінець): пресети й ручні хвилини прибрані.
 * [endMinuteOfDay] може бути ≥ 1440 лише для вже збереженого запису, що перетинає північ
 * (редагування) — тоді UI показує "наступного дня".
 *
 * За замовчуванням кінець = зараз, початок = на пів години раніше (але не раніше 00:00): найчастіше
 * людина заносить щойно завершену активність, тож форма одразу валідна.
 */
data class AddEntryUiState(
    val selectedCategoryId: String? = null, // FR-4.1: може прийти передвибраним з кнопки віджета
    val dateMillis: Long = startOfTodayMillis(),
    val startMinuteOfDay: Int = defaultStartMinute(),
    val endMinuteOfDay: Int = currentMinuteOfDay(),
    val note: String = "",
    val categoryRequiredError: Boolean = false,
    val overlapEntries: List<ActivityEntryEntity>? = null,
    val saved: Boolean = false
) {
    val durationMinutes: Int get() = endMinuteOfDay - startMinuteOfDay

    /** Кінець мусить бути пізніше початку — перевіряється наживо, не лише при збереженні. */
    val intervalValid: Boolean get() = durationMinutes > 0
}

private fun defaultStartMinute(): Int = (currentMinuteOfDay() - DEFAULT_INTERVAL_MINUTES).coerceAtLeast(0)

class AddEntryViewModel(
    categoryRepository: CategoryRepository,
    private val activityRepository: ActivityRepository,
    initialCategoryId: String? = null,
    private val editingEntryId: String? = null
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(AddEntryUiState(selectedCategoryId = initialCategoryId))
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()

    /** Історія на Stats (редагування наявного запису) — визначає заголовок екрана й кнопку "Видалити". */
    val isEditing: Boolean = editingEntryId != null

    init {
        // Попереднє заповнення форми даними наявного запису: тривалість показується інтервалом
        // (початок → початок + тривалість), який і є єдиним способом вводу часу.
        if (editingEntryId != null) {
            viewModelScope.launch {
                activityRepository.getById(editingEntryId)?.let { entry ->
                    _uiState.value = _uiState.value.copy(
                        selectedCategoryId = entry.categoryId,
                        dateMillis = localStartOfDay(entry.startTime),
                        startMinuteOfDay = minuteOfDay(entry.startTime),
                        endMinuteOfDay = minuteOfDay(entry.startTime) + entry.durationMinutes,
                        note = entry.note.orEmpty()
                    )
                }
            }
        }
    }

    /** Видалення наявного запису з екрана редагування (та сама дія, що "×" в історії на Stats). */
    fun deleteEntry() {
        val id = editingEntryId ?: return
        viewModelScope.launch {
            activityRepository.getById(id)?.let { activityRepository.delete(it) }
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, categoryRequiredError = false)
    }

    fun setDate(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(dateMillis = dateMillis)
    }

    /**
     * Зсув початку тягне за собою кінець із тією самою тривалістю (як у календарях): людина зазвичай
     * спершу ставить початок, а тривалість уже обдумана. Якщо тривалість була некоректна або новий
     * кінець вийшов би за межі доби — кінець не чіпаємо, і форма покаже помилку інтервалу.
     */
    fun setStartMinuteOfDay(minute: Int) {
        val state = _uiState.value
        val shiftedEnd = minute + state.durationMinutes
        val keepDuration = state.intervalValid && shiftedEnd <= MINUTES_IN_DAY - 1
        _uiState.value = state.copy(
            startMinuteOfDay = minute,
            endMinuteOfDay = if (keepDuration) shiftedEnd else state.endMinuteOfDay
        )
    }

    fun setEndMinuteOfDay(minute: Int) {
        _uiState.value = _uiState.value.copy(endMinuteOfDay = minute)
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
        if (!state.intervalValid) return

        val duration = state.durationMinutes.coerceAtLeast(1)
        val startTime = state.dateMillis + state.startMinuteOfDay * 60_000L

        // Редагування — той самий id, що в оригінальному записі: addEntry() вставляє з
        // OnConflictStrategy.REPLACE, тож це природно замінює саме цей рядок, а перевірка
        // перекриття (FR-1.3) вже виключає entry.id із власного результату — окремого
        // "update"-шляху не потрібно, ті самі правила, що для нового запису.
        val entry = if (editingEntryId != null) {
            ActivityEntryEntity(
                id = editingEntryId,
                categoryId = categoryId,
                startTime = startTime,
                durationMinutes = duration,
                note = state.note.ifBlank { null }
            )
        } else {
            ActivityEntryEntity(
                categoryId = categoryId,
                startTime = startTime,
                durationMinutes = duration,
                note = state.note.ifBlank { null }
            )
        }

        viewModelScope.launch {
            when (val result = activityRepository.addEntry(entry, forceOverwrite = forceOverwrite)) {
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
        private val initialCategoryId: String? = null,
        private val editingEntryId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddEntryViewModel(categoryRepository, activityRepository, initialCategoryId, editingEntryId) as T
    }
}
