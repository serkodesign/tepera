package com.serkodesign.tepera.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Категорія щойно збереженого запису — рядок цієї категорії на Home один раз м'яко підсвічується. */
object EntrySavedSignal {
    private val _categoryId = MutableStateFlow<String?>(null)
    val categoryId: StateFlow<String?> = _categoryId.asStateFlow()

    fun notify(categoryId: String) { _categoryId.value = categoryId }
    fun consume(categoryId: String) { if (_categoryId.value == categoryId) _categoryId.value = null }
}
