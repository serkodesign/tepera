package com.serkodesign.tepera.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.serkodesign.tepera.data.local.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Запит оцінки через Google Play In-App Review — системне вікно "оцініть застосунок" поверх Home,
 * без виходу з Tepera. Один раз за життя встановлення, не раніше [MIN_DAYS_SINCE_FIRST_LAUNCH] днів
 * після першого запуску (Monastic Style: жодного тиску й повторів).
 *
 * Прапорець "показано" ставиться ДО запуску: API не повідомляє, чи вікно справді з'явилось (Play сам
 * квотує показ), тож повторювати спроби немає сенсу. Для збірок поза Google Play (beta, debug) вікно
 * просто не відкриється. Пункт "Оцінити застосунок" в Налаштуваннях лишається прямим посиланням у
 * Play ([PlayStore.openListing]) — це явна дія людини, не контекстний запит.
 */
object ReviewPrompt {
    private const val TAG = "ReviewPrompt"
    private const val MIN_DAYS_SINCE_FIRST_LAUNCH = 14L
    private const val DAY_MS = 24L * 60 * 60 * 1000

    suspend fun maybeRequest(activity: Activity, settingsStore: SettingsStore) {
        val now = System.currentTimeMillis()
        val firstLaunch = settingsStore.firstLaunchMillis.first()
        if (firstLaunch == 0L || now - firstLaunch < MIN_DAYS_SINCE_FIRST_LAUNCH * DAY_MS) return
        if (settingsStore.reviewPromptShownAt.first() != 0L) return

        val manager = ReviewManagerFactory.create(activity)
        val info = runCatching { manager.requestReviewFlow().await() }
            .onFailure { Log.w(TAG, "requestReviewFlow failed", it) }
            .getOrNull() ?: return
        settingsStore.setReviewPromptShownAt(now)
        runCatching { manager.launchReviewFlow(activity, info).await() }
            .onFailure { Log.w(TAG, "launchReviewFlow failed", it) }
    }
}
