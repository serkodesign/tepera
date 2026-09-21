package com.serkodesign.tepera.widget

import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.util.DayPeriod
import com.serkodesign.tepera.util.currentDayPeriod

// FR-4.5: пріоритет nameKey для кожного періоду доби — робочі, не з SRS буквально (там лише
// вимога "порядок змінюється залежно від часу доби", без точних правил).
private val priorityByPeriod: Map<DayPeriod, List<String>> = mapOf(
    DayPeriod.MORNING to listOf("movement", "nature", "reading", "hobby", "social"),
    DayPeriod.DAY to listOf("reading", "hobby", "nature", "movement", "social"),
    DayPeriod.EVENING to listOf("social", "hobby", "reading", "nature", "movement"),
    DayPeriod.NIGHT to listOf("social", "hobby", "reading", "nature", "movement")
)

/**
 * FR-4.5: сортує активні категорії за пріоритетом поточного періоду доби. Кастомна категорія
 * (nameKey == null) не бере участі в "розумному" сортуванні — лишається в кінці списку,
 * у своєму початковому sortOrder.
 */
fun sortCategoriesForWidget(
    categories: List<CategoryEntity>,
    period: DayPeriod = currentDayPeriod()
): List<CategoryEntity> {
    val priority = priorityByPeriod.getValue(period)
    return categories.sortedBy { category ->
        val index = category.nameKey?.let { priority.indexOf(it) } ?: -1
        if (index == -1) Int.MAX_VALUE else index
    }
}

/**
 * Кнопки віджета: якщо користувач обрав категорії в налаштуваннях — рівно вони й у його порядку
 * (архівовані/видалені мовчки пропускаються), інакше автоматичне сортування FR-4.5.
 */
fun categoriesForWidget(
    active: List<CategoryEntity>,
    selectedIds: List<String>,
    period: DayPeriod = currentDayPeriod()
): List<CategoryEntity> {
    if (selectedIds.isEmpty()) return sortCategoriesForWidget(active, period)
    val byId = active.associateBy { it.id }
    val chosen = selectedIds.mapNotNull { byId[it] }
    return if (chosen.isEmpty()) sortCategoriesForWidget(active, period) else chosen
}
