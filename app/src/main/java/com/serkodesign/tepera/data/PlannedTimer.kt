package com.serkodesign.tepera.data

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val PLANNED_TIMER_WORK = "planned_timer"
private const val KEY_CATEGORY = "categoryId"
private const val KEY_START = "startTime"
private const val KEY_MINUTES = "minutes"
private const val PLANNED_TIMER_NOTIFICATION_ID = 4403

/**
 * Приймання підказки з категорією («Так» → «15 / 30 / 60 хв»): запускає звичайний тап-таймер категорії (той самий
 * `toggleCategoryTimer`: що вже йшло — зупиняється й зберігається) і ставить зупинку на обрану тривалість. Запис
 * з'являється лише тоді, коли таймер справді зупинено — людиною раніше (кнопка «Закінчив» на Home) або воркером
 * у строк. Одночасно живе одна така зупинка: нова замінює попередню.
 */
suspend fun startPlannedCategoryTimer(context: Context, categoryId: String, minutes: Int) {
    val app = context.applicationContext as TeperaApp
    if (!app.activeTimerStore.activeTimers.first().containsKey(categoryId)) {
        toggleCategoryTimer(app.activeTimerStore, app.activityRepository, categoryId)
    }
    val startTime = app.activeTimerStore.activeTimers.first()[categoryId] ?: return
    val request = OneTimeWorkRequestBuilder<PlannedTimerWorker>()
        .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
        .setInputData(workDataOf(KEY_CATEGORY to categoryId, KEY_START to startTime, KEY_MINUTES to minutes))
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(PLANNED_TIMER_WORK, ExistingWorkPolicy.REPLACE, request)
}

/** Зупиняє таймер у строк (лише якщо це той самий сеанс) і тихо повідомляє, скільки записано. */
class PlannedTimerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as TeperaApp
        val categoryId = inputData.getString(KEY_CATEGORY) ?: return Result.success()
        val startTime = inputData.getLong(KEY_START, 0L)
        val minutes = inputData.getInt(KEY_MINUTES, 0)
        // Людина вже зупинила чи перезапустила таймер — нічого не чіпаємо.
        if (app.activeTimerStore.activeTimers.first()[categoryId] != startTime) return Result.success()
        toggleCategoryTimer(app.activeTimerStore, app.activityRepository, categoryId)
        if (WeeklySummaryWorker.canNotify(applicationContext)) notifyDone(minutes)
        return Result.success()
    }

    private fun notifyDone(minutes: Int) {
        val open = android.app.PendingIntent.getActivity(
            applicationContext, 4411,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val text = applicationContext.getString(R.string.v3_sg_timer_done_text, minutes)
        val notification = NotificationCompat.Builder(applicationContext, NudgeChannel.ID)
            .setSmallIcon(R.drawable.ic_notification_summary)
            .setContentTitle(applicationContext.getString(R.string.v3_sg_timer_done_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(PLANNED_TIMER_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Дозвіл відкликали між перевіркою й відправкою — запис уже збережено, нічого не ламаємо.
        }
    }
}
