package com.serkodesign.tepera.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Щойно створені записи — для снекбара "Записано · Скасувати" в корені навігації. Форма додавання
 * закривається одразу після збереження, тож снекбар мусить жити вище за екрани, а не в самій формі.
 */
object SavedEntryUndo {

    data class Pending(val ids: List<String>)

    private val _pending = MutableStateFlow<Pending?>(null)
    val pending: StateFlow<Pending?> = _pending.asStateFlow()

    fun show(ids: List<String>) {
        if (ids.isNotEmpty()) _pending.value = Pending(ids)
    }

    fun clear() {
        _pending.value = null
    }
}
