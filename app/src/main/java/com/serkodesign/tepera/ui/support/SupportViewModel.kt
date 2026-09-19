package com.serkodesign.tepera.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serkodesign.tepera.data.billing.SupportProductsState
import com.serkodesign.tepera.data.billing.SupportPurchaseEvent
import com.serkodesign.tepera.data.billing.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Стан екрана підтримки: пакети offering `support`, обраний пакет і остання подія оплати. */
class SupportViewModel(private val repository: SupportRepository) : ViewModel() {

    val products: StateFlow<SupportProductsState> = repository.products

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    /** Остання подія оплати ("Дякую"/"очікує"/помилка); скасування показу не потребує (null). */
    private val _message = MutableStateFlow<SupportPurchaseEvent?>(null)
    val message: StateFlow<SupportPurchaseEvent?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                _message.value = if (event == SupportPurchaseEvent.Cancelled) null else event
            }
        }
    }

    fun refresh() = repository.refresh()

    fun select(packageId: String) {
        _selected.value = packageId
        _message.value = null
    }

    fun purchase() {
        val packageId = _selected.value ?: return
        _message.value = null
        repository.purchase(packageId)
    }

    class Factory(private val repository: SupportRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SupportViewModel(repository) as T
    }
}
