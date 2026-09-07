package com.serkodesign.tepera.widget

import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.util.DayPeriod
import com.serkodesign.tepera.util.currentDayPeriod

// FR-4.5: пріоритет nameKey для кожного періоду доби — робочі, не з SRS буквально (там лише
// вимога "порядок змінюється залежно від часу доби", без точних правил).
private val priorityByPeriod: Map<DayPeriod, List<String>> = mapOf(
    DayPeriod.MORNING to listOf("movement", "nature", "reading", "hobby", "sleep"),
    DayPeriod.DAY to listOf("reading", "hobby", "nature", "movement", "sleep"),
    DayPeriod.EVENING to listOf("hobby", "reading", "nature", "movement", "sleep"),
    DayPeriod.NIGHT to listOf("sleep", "hobby", "reading", "nature", "movement")
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

private const val NEGLECT_THRESHOLD_DAYS = 3

/** FR-4.6: категорію вважаємо занедбаною, якщо в ній не було запису понад 3 дні (або жодного). */
fun isNeglected(lastLoggedMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (lastLoggedMillis == null) return true
    val thresholdMillis = NEGLECT_THRESHOLD_DAYS * 24L * 60 * 60 * 1000
    return nowMillis - lastLoggedMillis > thresholdMillis
}
