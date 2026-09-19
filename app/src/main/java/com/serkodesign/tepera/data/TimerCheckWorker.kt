package com.serkodesign.tepera.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.category.categoryDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "timer_check"
private const val KEY_CATEGORY_ID = "category_id"
private const val KEY_START_TIME = "start_time"
private const val ACTION_STOP = "com.serkodesign.tepera.action.TIMER_CHECK_STOP"
private const val ACTION_DISMISS = "com.serkodesign.tepera.action.TIMER_CHECK_DISMISS"

private fun notificationIdFor(categoryId: String) = categoryId.hashCode()

/** Викликається раз при старті застосунку (TeperaApp.onCreate()) — ідемпотентно, як і решта
 * одноразового сідингу там. */
fun createTimerCheckNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.timer_check_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
    )
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}

/**
 * Тап по категорії почав таймер 4 год тому, і він досі йде (`ActiveTimerStore.start()` планує
 * цю перевірку через WorkManager, `ActiveTimerStore.stop()` скасовує) — Figma user-flow
 * (k6s4prQ9oK9x2uUvzHRghR, node 14:791) "Минуло 4 год? → так → Автозупинка". **За прямим
 * запитом користувача змінено відносно оригінальної схеми: ЛИШЕ сповіщення, БЕЗ автоматичної
 * зупинки таймера** — сам таймер продовжує йти, доки користувач не зупинить його вручну (тапом
 * по картці/віджету АБО кнопкою "Ні" в цьому сповіщенні). Рівно одна перевірка на сесію таймера
 * (без повторів, поки таймер і далі йде) — узгоджено з "Monastic Style" (без повторюваних
 * нагадувань), окреме пряме рішення користувача.
 */
class TimerCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val categoryId = inputData.getString(KEY_CATEGORY_ID) ?: return Result.success()
        val startTime = inputData.getLong(KEY_START_TIME, -1L)
        val app = applicationContext as TeperaApp

        // Таймер уже зупинили вручну (або перезапустили — інший startTime) до того, як минуло
        // 4 год, — саме так виглядає "не втручатись": перевірка сама нічого не робить.
        val currentStartTime = app.activeTimerStore.activeTimers.first()[categoryId]
        if (currentStartTime != startTime) return Result.success()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val category = app.categoryRepository.getById(categoryId) ?: return Result.success()
        val displayName = categoryDisplayName(category, applicationContext)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val dismissPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationIdFor(categoryId),
            Intent(applicationContext, TimerCheckNotificationReceiver::class.java)
                .setAction(ACTION_DISMISS)
                .putExtra(KEY_CATEGORY_ID, categoryId),
            flags
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationIdFor(categoryId) + 1,
            Intent(applicationContext, TimerCheckNotificationReceiver::class.java)
                .setAction(ACTION_STOP)
                .putExtra(KEY_CATEGORY_ID, categoryId),
            flags
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(displayName)
            .setContentText(applicationContext.getString(R.string.timer_check_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, applicationContext.getString(R.string.timer_check_action_yes), dismissPendingIntent)
            .addAction(0, applicationContext.getString(R.string.timer_check_action_no), stopPendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationIdFor(categoryId), notification)
        return Result.success()
    }

    companion object {
        private const val CHECK_DELAY_HOURS = 4L

        private fun uniqueWorkName(categoryId: String) = "timer_check_$categoryId"

        fun schedule(context: Context, categoryId: String, startTime: Long) {
            val data = Data.Builder()
                .putString(KEY_CATEGORY_ID, categoryId)
                .putLong(KEY_START_TIME, startTime)
                .build()
            val request = OneTimeWorkRequestBuilder<TimerCheckWorker>()
                .setInitialDelay(CHECK_DELAY_HOURS, TimeUnit.HOURS)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(uniqueWorkName(categoryId), ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context, categoryId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(categoryId))
        }
    }
}

/**
 * "Так" (усе ще цим займаюсь) лише закриває сповіщення — рівно одна перевірка, без повторів
 * (пряме рішення користувача). "Ні" зупиняє таймер і зберігає запис ТІЄЮ САМОЮ функцією, що
 * play/pause на картці категорії/віджеті (`toggleCategoryTimer`) — не окремий "автозупинка"-шлях,
 * той самий результат, що й ручна зупинка.
 */
class TimerCheckNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val categoryId = intent.getStringExtra(KEY_CATEGORY_ID) ?: return
        NotificationManagerCompat.from(context).cancel(notificationIdFor(categoryId))

        if (intent.action == ACTION_STOP) {
            val app = context.applicationContext as TeperaApp
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    toggleCategoryTimer(app.activeTimerStore, app.activityRepository, categoryId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
