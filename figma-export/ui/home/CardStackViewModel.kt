package com.serkodesign.tepera.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.cards.CardEngine
import com.serkodesign.tepera.data.cards.CardSource
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * T-13 (tepera-dev-spec.md), "рушій карток" — сполучна ланка між Home (яка вже збирає стан
 * кожної окремої картки через власні ViewModel) і чистим [CardEngine]: `HomeScreen` будує
 * список [CardSource] із уже зібраних `uiState` кожної картки (isDue/visible = [CardSource.
 * dataReady]) і викликає [evaluate] щоразу, коли будь-який із цих станів змінюється. Сам
 * [CardEngine] нічого не знає про конкретні картки — щоб додати новий тип, досить додати новий
 * рядок у список [CardSource] тут, у виклику з HomeScreen, без змін усередині рушія.
 */
class CardStackViewModel(private val cardHistoryRepository: CardHistoryRepository) : ViewModel() {

    private val engine = CardEngine(cardHistoryRepository)

    private val _visibleCards = MutableStateFlow<Set<CardType>>(emptySet())
    val visibleCards: StateFlow<Set<CardType>> = _visibleCards.asStateFlow()

    fun evaluate(sources: List<CardSource>) {
        viewModelScope.launch {
            val visible = engine.selectVisible(sources)
            _visibleCards.value = visible
            visible.forEach { type -> cardHistoryRepository.recordShown(type) }
        }
    }

    class Factory(private val cardHistoryRepository: CardHistoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CardStackViewModel(cardHistoryRepository) as T
    }
}
