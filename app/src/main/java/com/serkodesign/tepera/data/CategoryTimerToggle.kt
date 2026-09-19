package com.serkodesign.tepera.data

import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.repository.ActivityRepository
import kotlinx.coroutines.flow.first

/**
 * Спільна логіка тап-таймера категорії — використовується і на Home (HomeViewModel), і в кнопках
 * категорій на віджеті (ToggleCategoryTimerAction), і в "Ні" на сповіщенні (TimerCheckWorker), щоб
 * поведінка (округлення, forceOverwrite) не розходилась між місцями виклику.
 *
 * Перший виклик для категорії починає живий таймер, другий — зупиняє й зберігає запис. Ціла
 * хвилина зараховується лише після того, як вона повністю минула (floor, не round і без
 * примусового мінімуму 1 хв) — тап коротший за хвилину запис не створює взагалі.
 *
 * **Одночасно йде лише один таймер (за прямим запитом користувача, скасовує попередню можливість
 * таймити кілька категорій одразу):** старт нової категорії спершу зупиняє й зберігає таймер, що
 * вже йшов, — як на Home, так і на віджеті.
 */
suspend fun toggleCategoryTimer(
    activeTimerStore: ActiveTimerStore,
    activityRepository: ActivityRepository,
    categoryId: String
) {
    if (activeTimerStore.activeTimers.first().containsKey(categoryId)) {
        stopAndSave(activeTimerStore, activityRepository, categoryId)
    } else {
        // Спершу зупиняємо все, що вже йде (зазвичай одна категорія), і лише потім стартуємо нову.
        activeTimerStore.activeTimers.first().keys.forEach { runningId ->
            stopAndSave(activeTimerStore, activityRepository, runningId)
        }
        activeTimerStore.start(categoryId)
    }
    activeTimerStore.refreshWidgets()
}

/** Зупиняє таймер [categoryId] і зберігає запис, якщо минула хоча б одна повна хвилина. */
private suspend fun stopAndSave(
    activeTimerStore: ActiveTimerStore,
    activityRepository: ActivityRepository,
    categoryId: String
) {
    val startTime = activeTimerStore.stop(categoryId) ?: return
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
