package com.serkodesign.tepera.widget

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.analytics.track

/** FR-4.4: Quick Settings tile для швидкого логування — відкриває екран додавання запису. */
class QuickLogTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        track(com.serkodesign.tepera.analytics.AnalyticsEvent.QsTileAdded)
    }

    override fun onClick() {
        super.onClick()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_ADD_ENTRY, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            // Лише API < 34: з targetSdk 34+ старий виклик кидає виняток, тому вище вже PendingIntent.
            @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
