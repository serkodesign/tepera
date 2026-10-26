package com.serkodesign.tepera.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.isPausedAt
import com.serkodesign.tepera.domain.risk.NudgePlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * v3.0 MVP, Етап 4: планує ОДНЕ найближче нагадування перед годиною ризику через `AlarmManager` без точного будильника
 * (`setAndAllowWhileIdle`; `SCHEDULE_EXACT_ALARM` не потрібен) — система може зсунути момент на хвилини, це
 * вимірюється (`nudge_events`: запланований і фактичний час). Нічого не робить поза beta (`BuildConfig.V3_MVP`) і без
 * дозволу на сповіщення.
 *
 * Перепланування потрібне після будь-якої зміни, що впливає на розклад: збереження плану, вимкнення/ручне вікно,
 * перерахунок вікон, показ чи реакція на нагадування, перезавантаження, зміна часу або поясу. Повторний виклик
 * безпечний: попередній будильник замінюється.
 */
object NudgeScheduler {
    private const val REQUEST_CODE = 4401
    const val EXTRA_WINDOW_ID = "nudge_window_id"
    const val EXTRA_SCHEDULED_AT = "nudge_scheduled_at"

    suspend fun reschedule(context: Context, nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        if (!BuildConfig.V3_MVP) return@withContext
        val app = context.applicationContext as TeperaApp
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)

        val candidates = app.riskWindowRepository.getAll().map { w ->
            NudgePlanner.Candidate(
                w.id, w.startMinute, w.endMinute, w.dayType,
                enabled = !w.isUserDisabled && !w.isPausedAt(nowMillis), hasPlan = app.riskWindowRepository.getPlan(w.id) != null
            )
        }
        val blocked = app.nudgeEventDao.getShownSince(nowMillis - 2 * DAY_MILLIS)
            .map { Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() }.toSet()
        val next = if (WeeklySummaryWorker.canNotify(context)) NudgePlanner.next(candidates, now, blocked) else null

        val pending = pendingIntent(context, next?.windowId, next?.fireAt?.atZone(zone)?.toInstant()?.toEpochMilli())
        if (next == null) {
            alarms.cancel(pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.fireAt.atZone(zone).toInstant().toEpochMilli(), pending)
        }
    }

    private fun pendingIntent(context: Context, windowId: String?, scheduledAt: Long?): PendingIntent {
        val intent = Intent(context, NudgeReceiver::class.java).apply {
            windowId?.let { putExtra(EXTRA_WINDOW_ID, it) }
            scheduledAt?.let { putExtra(EXTRA_SCHEDULED_AT, it) }
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
