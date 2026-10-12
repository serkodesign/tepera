package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.cards.CardResult
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import com.serkodesign.tepera.data.repository.WelcomeBackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WelcomeBackUiState(
    val visible: Boolean = false,
    val days: Int = 0,
    val loggedMinutes: Int = 0,
    val averageOnlineMinutes: Int? = null
)

/**
 * CC-4: підсумок після перерви ≥ 3 діб. Стежить за `welcomeBackPendingFrom` у налаштуваннях — його виставляє
 * [WelcomeBackRepository.onAppOpened] при відкритті застосунку, тож картка з'являється незалежно від того, що
 * швидше: відкриття чи перший запис стану Home.
 */
class WelcomeBackViewModel(
    private val repository: WelcomeBackRepository,
    private val settingsStore: SettingsStore,
    private val cardHistoryRepository: CardHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeBackUiState())
    val uiState: StateFlow<WelcomeBackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.welcomeBackPendingFrom.collect { pendingFrom ->
                _uiState.value = if (pendingFrom <= 0L) {
                    WelcomeBackUiState()
                } else {
                    repository.currentSummary()?.let {
                        WelcomeBackUiState(true, it.days, it.loggedMinutes, it.averageOnlineMinutes)
                    } ?: WelcomeBackUiState()
                }
            }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            cardHistoryRepository.recordResolved(CardType.WELCOME_BACK, CardResult.SKIPPED)
            repository.dismiss()
        }
    }

    class Factory(
        private val repository: WelcomeBackRepository,
        private val settingsStore: SettingsStore,
        private val cardHistoryRepository: CardHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WelcomeBackViewModel(repository, settingsStore, cardHistoryRepository) as T
    }
}
