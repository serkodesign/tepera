package com.serkodesign.tepera.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.ScanOutcome
import java.time.Instant
import java.time.ZoneId

/** Номер єдиного сповіщення «я знайшов твою годину» (окремо від нагадування про план, [NUDGE_NOTIFICATION_ID]). */
private const val FOUND_NOTIFICATION_ID = 4403

/**
 * v3.0 MVP, сценарій 1 (день 8): ОДНЕ сповіщення «Я знайшов твою годину», коли тиждень спостереження дав перші вікна.
 * Тихий канал нагадувань, без звуку. Показується рівно раз за життя встановлення (прапорець ставиться і тоді, коли
 * сповіщення вимкнені, щоб воно не прийшло «із запізненням» через тижні) і не в день, коли вже було нагадування про
 * план — інакше з'явилося б два сповіщення за добу.
 */
object FoundWindowsNotifier {
    private const val PREFS = "v3_found_windows"
    private const val KEY_DONE = "done"

    /** Людина сама відкрила Home і побачила знайдені вікна — сповіщення про них уже зайве. */
    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
    }

    suspend fun maybeNotify(context: Context, outcome: ScanOutcome) {
        if (outcome !is ScanOutcome.Done) return
        if (outcome.windows.none { !it.isManual && !it.isUserDisabled }) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return

        val app = context.applicationContext as TeperaApp
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val startOfToday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        if (app.nudgeEventDao.getShownSince(startOfToday).isNotEmpty()) return // спробуємо наступного перерахунку

        prefs.edit().putBoolean(KEY_DONE, true).apply()
        if (!WeeklySummaryWorker.canNotify(context)) return
        NudgeChannel.create(context)
        val open = PendingIntent.getActivity(
            context, 4411,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NudgeChannel.ID)
            .setSmallIcon(R.drawable.ic_notification_summary)
            .setContentTitle(context.getString(R.string.v3_found_title))
            .setContentText(context.getString(R.string.v3_found_body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(FOUND_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Дозвіл відкликали між перевіркою й відправкою — нічого не ламаємо.
        }
    }
}
