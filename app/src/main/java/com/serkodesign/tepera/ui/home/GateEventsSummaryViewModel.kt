package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.cards.CardResult
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import com.serkodesign.tepera.data.repository.GateEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GateEventsSummaryUiState(
    val isDue: Boolean = false,
    val cancelledCount: Int = 0
)

/**
 * T-6 (tepera-dev-spec.md), FR-P.3 "свідчення компетентності замість натхнення": "тихі факти
 * власної здатності" замість мотиваційних цитат. Буквальна вимога приймання: "формулювання лише
 * фактичне... жодних стріків, жодного співвідношення пройдено/скасовано, жодного показу
 * PROCEEDED як невдач" — ця картка рахує ТІЛЬКИ [GateEventRepository.countCancelledThisMonth],
 * не торкається PROCEEDED узагалі, навіть щоб порівняти.
 *
 * Частота — "не частіше разу на місяць" (документ) — рушій T-13 через `minIntervalDays = 30` у
 * `CardSource`, [dismiss] пише той самий SKIPPED-запис, що решта карток без питання/відповіді.
 */
class GateEventsSummaryViewModel(
    private val gateEventRepository: GateEventRepository,
    private val cardHistoryRepository: CardHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GateEventsSummaryUiState())
    val uiState: StateFlow<GateEventsSummaryUiState> = _uiState.asStateFlow()

    init {
        checkDataReady()
    }

    private fun checkDataReady() {
        viewModelScope.launch {
            val count = gateEventRepository.countCancelledThisMonth()
            if (count > 0) {
                _uiState.value = GateEventsSummaryUiState(isDue = true, cancelledCount = count)
            }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            cardHistoryRepository.recordResolved(CardType.GATE_EVENTS_SUMMARY, CardResult.SKIPPED)
            _uiState.value = GateEventsSummaryUiState()
        }
    }

    class Factory(
        private val gateEventRepository: GateEventRepository,
        private val cardHistoryRepository: CardHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GateEventsSummaryViewModel(gateEventRepository, cardHistoryRepository) as T
    }
}
