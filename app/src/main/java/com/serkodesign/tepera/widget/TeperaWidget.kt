package com.serkodesign.tepera.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import android.content.Context

/**
 * Компактна 4x1 і розширена 4x2 через SizeMode.Responsive (FR-4.1–4.2). Розміри-заглушки
 * нижче — повна реалізація (кнопки категорій, шкала балансу) належить Фазі 3.
 */
class TeperaWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 40.dp),
            DpSize(180.dp, 80.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(modifier = GlanceModifier.fillMaxSize()) {
                Text(text = "Tepera")
            }
        }
    }
}

class TeperaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget()
}
