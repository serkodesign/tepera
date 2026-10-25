package com.serkodesign.tepera.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.NudgeAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * v3.0 MVP, Етап 4: кнопки сповіщення «Добре» і «Не сьогодні». Обидві лише записують реакцію в `nudge_events` і
 * прибирають сповіщення; «Не сьогодні» наслідків не має (нагадування й так не більше одного на добу, вікно лишається
 * ввімкненим). Жодного підтвердження чи другого запиту.
 */
class NudgeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val action = runCatching { NudgeAction.valueOf(intent.getStringExtra(EXTRA_ACTION).orEmpty()) }.getOrNull() ?: return
        NotificationManagerCompat.from(context).cancel(NUDGE_NOTIFICATION_ID)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                (context.applicationContext as TeperaApp).nudgeEventDao.setAction(eventId, action)
                NudgeScheduler.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_EVENT_ID = "nudge_event_id"
        private const val EXTRA_ACTION = "nudge_action"

        fun pending(context: Context, eventId: String, action: NudgeAction): PendingIntent {
            val intent = Intent(context, NudgeActionReceiver::class.java)
                .putExtra(EXTRA_EVENT_ID, eventId)
                .putExtra(EXTRA_ACTION, action.name)
            // Окремий requestCode на дію, інакше система вважала б обидва PendingIntent однаковими.
            val code = 4420 + action.ordinal
            return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
