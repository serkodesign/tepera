package com.serkodesign.tepera.analytics

import android.content.Context
import com.serkodesign.tepera.TeperaApp

/** Аналітика застосунку з будь-якого Context (сервіси, ресівери, util); `NoOp`, якщо Application не Tepera (тести). */
val Context.analytics: Analytics
    get() = (applicationContext as? TeperaApp)?.analytics ?: NoOpAnalytics

fun Context.track(event: AnalyticsEvent) = analytics.track(event)
