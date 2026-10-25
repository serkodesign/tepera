package com.serkodesign.tepera.ui.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.PartOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Дані для L5: вікно, на яке складається план, і вже збережений план (якщо є). */
data class PlanEditorUi(
    val windowId: String,
    val startMinute: Int,
    val dayType: DayType,
    val recurrenceDays: Int,
    val observedDays: Int,
    val partOfDay: PartOfDay,
    val existingIf: String?,
    val existingThen: String?
)

class PlanEditorViewModel(private val app: TeperaApp, private val windowId: String) : ViewModel() {
    private val _ui = MutableStateFlow<PlanEditorUi?>(null)
    val ui: StateFlow<PlanEditorUi?> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val window = app.riskWindowRepository.getWindow(windowId) ?: return@launch
            val plan = app.riskWindowRepository.getPlan(windowId)
            _ui.value = PlanEditorUi(
                windowId = window.id, startMinute = window.startMinute, dayType = window.dayType,
                recurrenceDays = window.recurrenceDays, observedDays = window.observedDays,
                partOfDay = HeatGrid.partOfDay(window.startMinute),
                existingIf = plan?.ifText, existingThen = plan?.thenText
            )
        }
    }

    suspend fun save(ifText: String, thenText: String) {
        app.riskWindowRepository.savePlan(windowId, ifText, thenText)
    }

    class Factory(private val app: TeperaApp, private val windowId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanEditorViewModel(app, windowId) as T
    }
}
