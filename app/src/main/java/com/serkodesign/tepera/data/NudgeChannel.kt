package com.serkodesign.tepera.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.serkodesign.tepera.R

/**
 * v3.0 MVP, Етап 4: канал «Нагадування про план». Низька важливість, без звуку й вібрації: нагадування має бути
 * тихим і не стати власним тригером. Планування й показ нагадувань тут ще не реалізовані.
 */
object NudgeChannel {
    const val ID = "nudge_plan"

    /** Ідемпотентно: повторне створення існуючого каналу нічого не змінює. */
    fun create(context: Context) {
        val channel = NotificationChannel(ID, context.getString(R.string.v3_nudge_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
