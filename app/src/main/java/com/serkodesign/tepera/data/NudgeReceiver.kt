package com.serkodesign.tepera.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.isPausedAt
import com.serkodesign.tepera.data.local.entity.NudgeAction
import com.serkodesign.tepera.data.local.entity.NudgeEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/** Номер єдиного сповіщення «нагадування про план»: нове замінює попереднє. */
internal const val NUDGE_NOTIFICATION_ID = 4402

/**
 * v3.0 MVP, Етап 4: спрацювання будильника — вирішує, чи показувати нагадування, показує його (канал «Нагадування про
 * план», низька важливість, без звуку) й планує наступне; текст — підказка, що зробити замість стрічки
 * ([SuggestionPicker]). Не показує, якщо: нема дозволу, вікно вимкнене чи на паузі,
 * сьогодні вже показували (одне на добу), людина зараз у стрічці, або будильник запізнився так, що вікно вже
 * почалось. Кожне спрацювання пишеться в `nudge_events`: з фактичним часом показу (для виміру похибки) або без нього.
 */
class NudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val windowId = intent.getStringExtra(NudgeScheduler.EXTRA_WINDOW_ID)
                val scheduledAt = intent.getLongExtra(NudgeScheduler.EXTRA_SCHEDULED_AT, 0L)
                if (windowId != null && scheduledAt > 0) handle(context, windowId, scheduledAt)
                NudgeScheduler.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, windowId: String, scheduledAt: Long) {
        val app = context.applicationContext as TeperaApp
        val now = System.currentTimeMillis()
        val window = app.riskWindowRepository.getWindow(windowId) ?: return
        if (window.isUserDisabled || window.isPausedAt(now)) return
        // Будильник міг бути виставлений ще до перевірки тижня спостереження (оновлення поверх старої збірки).
        if (app.database.observationPeriodDao().getLatest() == null) return

        val zone = ZoneId.systemDefault()
        val startOfToday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val shownToday = app.nudgeEventDao.getShownSince(startOfToday).isNotEmpty()
        val windowStartedAlready = now > scheduledAt + NudgeLead.LATE_TOLERANCE_MILLIS
        val allowed = WeeklySummaryWorker.canNotify(context) && !shownToday && !windowStartedAlready &&
            !app.usageSlotRepository.isInFeedNow(now)

        val event = NudgeEventEntity(
            id = UUID.randomUUID().toString(), windowId = windowId, scheduledAt = scheduledAt,
            shownAt = if (allowed) now else null, action = NudgeAction.NONE
        )
        app.nudgeEventDao.insert(event)
        if (allowed) {
            val suggestion = SuggestionPicker.pickFor(context, windowId, nowMillis = now)
            if (suggestion != null) show(context, event, window.startMinute, context.getString(suggestionTextRes(suggestion.id)))
        }
    }

    companion object {
        /**
         * Для перевірки на собі (екран тестувальників): показує нагадування негайно для найближчого ввімкненого вікна,
         * минаючи правила «одне на добу» й «за 10 хв до». Подія пишеться в `nudge_events` як справжня, тож кнопки
         * «Добре»/«Не сьогодні» теж працюють. Повертає `false`, якщо нема вікна або сповіщення вимкнені.
         */
        suspend fun fireNow(context: Context): Boolean {
            val app = context.applicationContext as TeperaApp
            if (!WeeklySummaryWorker.canNotify(context)) return false
            val now = System.currentTimeMillis()
            val window = app.riskWindowRepository.getAll().firstOrNull { !it.isUserDisabled && !it.isPausedAt(now) } ?: return false
            val suggestion = SuggestionPicker.pickFor(context, window.id, nowMillis = now) ?: return false
            val event = NudgeEventEntity(
                id = UUID.randomUUID().toString(), windowId = window.id, scheduledAt = now, shownAt = now, action = NudgeAction.NONE
            )
            app.nudgeEventDao.insert(event)
            show(context, event, window.startMinute, context.getString(suggestionTextRes(suggestion.id)))
            return true
        }

        private fun show(context: Context, event: NudgeEventEntity, startMinute: Int, text: String) {
        val open = PendingIntent.getActivity(
            context, 4410,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val time = String.format(Locale.ROOT, "%02d:%02d", startMinute / 60 % 24, startMinute % 60)
        val builder = NotificationCompat.Builder(context, NudgeChannel.ID)
            .setSmallIcon(R.drawable.ic_notification_summary)
            .setContentTitle(context.getString(R.string.v3_nudge_notification_title, time))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.v3_nudge_action_ok), NudgeActionReceiver.pending(context, event.id, NudgeAction.OK))
        try {
            NotificationManagerCompat.from(context).notify(NUDGE_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Дозвіл відкликали між перевіркою й відправкою — нічого не ламаємо.
        }
        }
    }
}

internal object NudgeLead {
    /** Будильник без точності може спізнитись; пізніше за це нагадувати вже нема сенсу — вікно почалось. */
    const val LATE_TOLERANCE_MILLIS = 12 * 60_000L
}
