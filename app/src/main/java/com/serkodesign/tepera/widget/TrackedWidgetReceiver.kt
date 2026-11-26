package com.serkodesign.tepera.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.serkodesign.tepera.analytics.AnalyticsEvent
import com.serkodesign.tepera.analytics.WidgetKind
import com.serkodesign.tepera.analytics.track

/**
 * Базовий ресівер віджета: `onEnabled` викликається, коли додано ПЕРШИЙ екземпляр цього типу віджета
 * (наступні екземпляри того ж типу окремої події не дають) — це і є `widget_added`.
 */
abstract class TrackedWidgetReceiver(private val kind: WidgetKind) : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.track(AnalyticsEvent.WidgetAdded(kind))
    }
}
