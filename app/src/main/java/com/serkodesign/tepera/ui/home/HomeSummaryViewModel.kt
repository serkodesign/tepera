package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.repository.DaySummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Дані для тізерів і шторки підсумків Home FINAL (docs/HANDOFF.md, розділи 5 і 8): завершені доби за останні
 * ~35 днів у термінах "без телефону" ([DayStat]). Джерело — `day_summaries` ([DaySummaryRepository]); перед
 * читанням Home дозаписує завершені доби (ідемпотентно — те саме, що раз на ~30 хв робить віджет-воркер),
 * тож "Вчора" з'являється одразу, а не після наступного оновлення віджета.
 */
class HomeSummaryViewModel(private val repository: DaySummaryRepository) : ViewModel() {

    private val _days = MutableStateFlow<Map<LocalDate, DayStat>>(emptyMap())
    val days: StateFlow<Map<LocalDate, DayStat>> = _days

    /** Викликається при вході на Home (LifecycleResumeEffect). Збій дозапису не заважає прочитати вже наявне. */
    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.backfillCompletedDays() }
            val today = LocalDate.now()
            val rows = runCatching { repository.getRange(today.minusDays(HISTORY_DAYS), today.minusDays(1)) }.getOrDefault(emptyList())
            _days.value = rows.mapNotNull(::dayStatFromSummary).associateBy { it.date }
        }
    }

    class Factory(private val repository: DaySummaryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeSummaryViewModel(repository) as T
    }

    private companion object {
        // Місяць (30 діб) + запас на початок календарного місяця у шторці.
        const val HISTORY_DAYS = 35L
    }
}
