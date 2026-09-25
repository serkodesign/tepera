package com.serkodesign.tepera.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.util.WeeklySummarySchedule
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "weekly_summary"
private const val UNIQUE_WORK_NAME = "weekly_summary"
private const val NOTIFICATION_ID = 4801

/**
 * CC-8: єдине сповіщення застосунку — раз на тиждень (неділя, 19:00), лише якщо людина сама ввімкнула
 * тижневий підсумок у Налаштуваннях. Текст статичний: у фоні нічого не рахується й не зчитується, лише
 * нагадується, що підсумок є. Тап відкриває Home з карткою «Цей тиждень» ([MainActivity.EXTRA_OPEN_WEEKLY_SUMMARY]).
 * Після відправлення воркер сам планує наступне сповіщення.
 */
class WeeklySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TeperaApp
        if (!app.settingsStore.weeklySummaryEnabled.first()) return Result.success() // вимкнено — не планує далі
        if (canNotify(applicationContext)) notifyNow(applicationContext)
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        /** Канал створюється при старті застосунку (ідемпотентно). */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.weekly_summary_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }

        /** Android 13+: дозвіл на сповіщення; нижче — сповіщення дозволені за замовчуванням. */
        fun canNotify(context: Context): Boolean =
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()

        /** Планує найближче сповіщення (наступна неділя 19:00); повторний виклик замінює попереднє планування. */
        fun schedule(context: Context) {
            val delay = (WeeklySummarySchedule.nextRunMillis(System.currentTimeMillis()) - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<WeeklySummaryWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /** Якщо наступне сповіщення ще не заплановане (напр. після оновлення) — планує; наявне не чіпає. */
        fun ensureScheduled(context: Context) {
            val delay = (WeeklySummarySchedule.nextRunMillis(System.currentTimeMillis()) - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<WeeklySummaryWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }

        private fun notifyNow(context: Context) {
            val open = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_WEEKLY_SUMMARY, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_summary)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.weekly_summary_notification_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // Дозвіл відкликали між перевіркою й відправкою — нічого не ламаємо.
            }
        }
    }
}
