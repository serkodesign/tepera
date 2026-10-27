package com.serkodesign.tepera.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.util.seriesRanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import java.time.LocalDate
import java.time.ZoneId

data class CalendarUiState(
    val stickers: Map<LocalDate, List<CalendarSticker>> = emptyMap(),
    /** Початок і кінець цілої багатодобової активності за `seriesId` (для підпису в рядку запису). */
    val seriesRanges: Map<String, Pair<Long, Long>> = emptyMap()
)

class CalendarViewModel(
    activityRepository: ActivityRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val mode = MutableStateFlow(CalendarMode.MONTH)
    val anchor = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        activityRepository.observeEntriesInRange(0, Long.MAX_VALUE),
        categoryRepository.observeAllCategories()
    ) { entries, categories ->
        val all = if (PREVIEW_FAKE_STICKERS) entries + previewEntries() else entries
        CalendarUiState(groupStickersByDay(all, categories), seriesRanges(all))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun setMode(value: CalendarMode) {
        mode.value = value
    }

    /** [direction] -1 / +1: на місяць або тиждень залежно від режиму. */
    fun step(direction: Int) {
        anchor.value = when (mode.value) {
            CalendarMode.MONTH -> anchor.value.withDayOfMonth(1).plusMonths(direction.toLong())
            CalendarMode.WEEK -> anchor.value.plusWeeks(direction.toLong())
        }
    }

    fun goToToday() {
        anchor.value = LocalDate.now()
    }

    class Factory(
        private val activityRepository: ActivityRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalendarViewModel(activityRepository, categoryRepository) as T
    }
}

/** ТИМЧАСОВО (PREVIEW_FAKE_STICKERS): 6 записів на 27-ге, 5 на 26-те і 3 на 28-ме поточного місяця — лише для перегляду. */
private fun previewEntries(): List<ActivityEntryEntity> {
    val cats = listOf("default-nature", "default-reading", "default-hobby", "default-movement", "default-social", "default-errands")
    val month = LocalDate.now().withDayOfMonth(1)
    val zone = ZoneId.systemDefault()
    fun day(d: Int, n: Int) = List(n) { i ->
        val start = month.withDayOfMonth(d).atTime(7 + i * 2, 0).atZone(zone).toInstant().toEpochMilli()
        ActivityEntryEntity(
            id = "preview-$d-$i", categoryId = cats[i % cats.size], startTime = start, durationMinutes = 30 + i * 15,
            note = null, source = EntrySource.MANUAL, seriesId = null, createdAt = start
        )
    }
    return day(27, 6) + day(26, 5) + day(28, 3)
}
