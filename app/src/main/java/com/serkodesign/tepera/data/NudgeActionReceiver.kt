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
 * v3.0 MVP, Етап 4: кнопка сповіщення «Добре» записує реакцію в `nudge_events` і прибирає сповіщення. Кнопки «Не сьогодні»
 * більше нема: вимкнути план з самого сповіщення чи листа не можна, лише зсередини Tepera (інакше за день-два це стало б
 * рефлексом). Варіант NOT_TODAY в enum лишається, бо старі записи в БД його містять.
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
