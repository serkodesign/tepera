package com.serkodesign.tepera.data

import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository

/**
 * Спільна логіка тап-таймера категорії — використовується і на Home (HomeViewModel), і в кнопках
 * категорій на віджеті (ToggleCategoryTimerAction), щоб поведінка (округлення, forceOverwrite)
 * не розходилась між двома місцями виклику.
 *
 * Перший виклик для категорії починає живий таймер, другий — зупиняє й зберігає запис. Ціла
 * хвилина зараховується лише після того, як вона повністю минула (floor, не round і без
 * примусового мінімуму 1 хв) — тап коротший за хвилину запис не створює взагалі.
 */
suspend fun toggleCategoryTimer(
    activeTimerStore: ActiveTimerStore,
    activityRepository: ActivityRepository,
    categoryId: String
) {
    val startTime = activeTimerStore.stop(categoryId)
    if (startTime == null) {
        activeTimerStore.start(categoryId)
    } else {
        val minutes = ((System.currentTimeMillis() - startTime) / 60_000L).toInt()
        if (minutes > 0) {
            // forceOverwrite: зупинка живого таймера — швидка дія без діалогів; overlap-перевірка
            // (FR-1.4) створена для ручного вводу, тут би лише заважала непередбачувано.
            activityRepository.addEntry(
                ActivityEntryEntity(
                    categoryId = categoryId,
                    startTime = startTime,
                    durationMinutes = minutes
                ),
                forceOverwrite = true
            )
        }
    }
}
