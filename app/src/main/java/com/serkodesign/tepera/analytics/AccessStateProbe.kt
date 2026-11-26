package com.serkodesign.tepera.analytics

import android.content.Context

/** Подію «стало доступно» шлемо лише при переході false → true. */
internal fun becameTrue(previous: Boolean, now: Boolean): Boolean = !previous && now

/**
 * Порівнює поточний стан дозволів із запам'ятованим і шле `usage_access_granted` / `accessibility_service_enabled`
 * при переході «ні → так». Це власна перевірка системних налаштувань (категорія A), а не дані, зібрані сервісом.
 * Викликається при поверненні в застосунок (`MainActivity.onResume`).
 */
object AccessStateProbe {
    private const val PREFS = "analytics_state"
    private const val KEY_USAGE = "usage_access"
    private const val KEY_A11Y = "a11y_enabled"

    fun probe(context: Context, usageAccess: Boolean, a11yEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (becameTrue(prefs.getBoolean(KEY_USAGE, false), usageAccess)) context.track(AnalyticsEvent.UsageAccessGranted)
        if (becameTrue(prefs.getBoolean(KEY_A11Y, false), a11yEnabled)) context.track(AnalyticsEvent.AccessibilityServiceEnabled)
        prefs.edit().putBoolean(KEY_USAGE, usageAccess).putBoolean(KEY_A11Y, a11yEnabled).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
