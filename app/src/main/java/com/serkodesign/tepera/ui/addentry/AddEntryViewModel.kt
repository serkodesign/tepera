package com.serkodesign.tepera.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.SaveEntryResult
import com.serkodesign.tepera.util.localStartOfDay
import com.serkodesign.tepera.util.minuteOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private const val MAX_NOTE_LENGTH = 250
private const val DEFAULT_INTERVAL_MINUTES = 30
private const val MINUTE_MILLIS = 60_000L

/** Найдовша активність, яку можна занести одним записом. Захист від випадкової помилки в даті. */
const val MAX_INTERVAL_DAYS = 7
private const val MAX_INTERVAL_MINUTES = MAX_INTERVAL_DAYS * 24 * 60

enum class IntervalError { NONE, END_BEFORE_START, TOO_LONG }

/**
 * Час запису вводиться ЛИШЕ інтервалом: повний момент початку й кінця (дата + час), до
 * [MAX_INTERVAL_DAYS] діб. Активність, що перетинає межу доби, ділиться на частини при збереженні
 * (див. `ActivityRepository.saveInterval`), для форми це один запис.
 *
 * За замовчуванням кінець = зараз, початок = на пів години раніше (може бути й учора о 00:10):
 * найчастіше людина заносить щойно завершену активність, тож форма одразу валідна.
 */
data class AddEntryUiState(
    val selectedCategoryId: String? = null, // FR-4.1: може прийти передвибраним з кнопки віджета
    val startMillis: Long = defaultEndMillis() - DEFAULT_INTERVAL_MINUTES * MINUTE_MILLIS,
    val endMillis: Long = defaultEndMillis(),
    val note: String = "",
    val categoryRequiredError: Boolean = false,
    val overlapEntries: List<ActivityEntryEntity>? = null,
    val saved: Boolean = false
) {
    val durationMinutes: Int get() = ((endMillis - startMillis) / MINUTE_MILLIS).toInt()

    val intervalError: IntervalError
        get() = when {
            durationMinutes <= 0 -> IntervalError.END_BEFORE_START
            durationMinutes > MAX_INTERVAL_MINUTES -> IntervalError.TOO_LONG
            else -> IntervalError.NONE
        }

    val intervalValid: Boolean get() = intervalError == IntervalError.NONE

    val startDayMillis: Long get() = localStartOfDay(startMillis)
    val endDayMillis: Long get() = localStartOfDay(endMillis)
    val startMinuteOfDay: Int get() = minuteOfDay(startMillis)
    val endMinuteOfDay: Int get() = minuteOfDay(endMillis)

    /** Остання дата, яку можна обрати для кінця. */
    val maxEndDayMillis: Long get() = addDays(startDayMillis, MAX_INTERVAL_DAYS)
}

private fun defaultEndMillis(): Long = System.currentTimeMillis().let { it - it % MINUTE_MILLIS }

private fun addDays(dayMillis: Long, days: Int): Long =
    Calendar.getInstance().apply { timeInMillis = dayMillis; add(Calendar.DAY_OF_YEAR, days) }.timeInMillis

/** [dayMillis] (локальна північ) + [minuteOfDay]; через Calendar, а не "+ хвилини*60000" — щоб не ламала зміна часу. */
private fun atMinute(dayMillis: Long, minuteOfDay: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
    }.timeInMillis

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

    // Записи активності, що редагується (у багатодобової їх кілька) — замінюються при збереженні.
    private var editingIds: List<String> = emptyList()
    private var editingSeriesId: String? = null

    init {
        // Попереднє заповнення форми наявною активністю: усі її частини зливаються назад в один
        // інтервал (від початку першої до кінця останньої).
        if (editingEntryId != null) {
            viewModelScope.launch {
                val pieces = activityRepository.getWholeActivity(editingEntryId).sortedBy { it.startTime }
                val first = pieces.firstOrNull() ?: return@launch
                editingIds = pieces.map { it.id }
                editingSeriesId = first.seriesId
                _uiState.value = _uiState.value.copy(
                    selectedCategoryId = first.categoryId,
                    startMillis = first.startTime,
                    endMillis = pieces.maxOf { it.startTime + it.durationMinutes * MINUTE_MILLIS },
                    note = first.note.orEmpty()
                )
            }
        }
    }

    /** Видалення наявної активності (з усіма її частинами) з екрана редагування. */
    fun deleteEntry() {
        val id = editingEntryId ?: return
        viewModelScope.launch {
            activityRepository.getById(id)?.let { activityRepository.deleteWholeActivity(it) }
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, categoryRequiredError = false)
    }

    /**
     * Зміна початку (дати чи часу) тягне за собою кінець із тією самою тривалістю, як у календарях:
     * людина зазвичай спершу ставить початок, а тривалість уже обдумана. Якщо інтервал зараз
     * некоректний — кінець не чіпаємо.
     */
    private fun moveStart(newStart: Long) {
        val state = _uiState.value
        val newEnd = if (state.intervalValid) state.endMillis + (newStart - state.startMillis) else state.endMillis
        _uiState.value = state.copy(startMillis = newStart, endMillis = newEnd)
    }

    fun setStartDate(dayMillis: Long) {
        moveStart(atMinute(dayMillis, _uiState.value.startMinuteOfDay))
    }

    fun setStartMinuteOfDay(minute: Int) {
        moveStart(atMinute(_uiState.value.startDayMillis, minute))
    }

    fun setEndDate(dayMillis: Long) {
        val state = _uiState.value
        _uiState.value = state.copy(endMillis = atMinute(dayMillis, state.endMinuteOfDay))
    }

    fun setEndMinuteOfDay(minute: Int) {
        val state = _uiState.value
        _uiState.value = state.copy(endMillis = atMinute(state.endDayMillis, minute))
    }

    /** Один тап для найчастішого випадку: "закінчив наступного дня" (23:00 → 01:00). */
    fun endNextDay() {
        setEndDate(addDays(_uiState.value.endDayMillis, 1))
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

        viewModelScope.launch {
            val result = activityRepository.saveInterval(
                categoryId = categoryId,
                startMillis = state.startMillis,
                endMillis = state.endMillis,
                note = state.note.ifBlank { null },
                replaceIds = editingIds,
                seriesId = editingSeriesId,
                forceOverwrite = forceOverwrite
            )
            when (result) {
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
