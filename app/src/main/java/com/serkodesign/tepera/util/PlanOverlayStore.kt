package com.serkodesign.tepera.util

import android.content.Context

/**
 * v3.0 MVP, Етап 8 (лише beta): налаштування й лічильники «плану поверх стрічки». SharedPreferences, бо читається з
 * сервісу доступності синхронно, а лічильники не потрапляють у Room (міграція не потрібна, в експорт не входять).
 * Лічильники — лише для екрана тестувальників: скільки разів план показано і що людина зробила.
 */
object PlanOverlayStore {
    private const val PREFS = "plan_overlay"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SHOWN = "shown"
    private const val KEY_CONTINUED = "continued"
    private const val KEY_LEFT = "left"
    private const val KEY_LAST_PREFIX = "last_"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** За замовчуванням увімкнено: це суть режиму тесту; сервіс доступності й так потребує окремого дозволу. */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun lastShownMillis(context: Context, windowId: String): Long = prefs(context).getLong(KEY_LAST_PREFIX + windowId, 0L)

    fun recordShown(context: Context, windowId: String, nowMillis: Long = System.currentTimeMillis()) {
        val p = prefs(context)
        p.edit().putLong(KEY_LAST_PREFIX + windowId, nowMillis).putInt(KEY_SHOWN, p.getInt(KEY_SHOWN, 0) + 1).apply()
    }

    fun recordContinued(context: Context) = bump(context, KEY_CONTINUED)

    fun recordLeft(context: Context) = bump(context, KEY_LEFT)

    fun shownCount(context: Context): Int = prefs(context).getInt(KEY_SHOWN, 0)
    fun continuedCount(context: Context): Int = prefs(context).getInt(KEY_CONTINUED, 0)
    fun leftCount(context: Context): Int = prefs(context).getInt(KEY_LEFT, 0)

    private fun bump(context: Context, key: String) {
        val p = prefs(context)
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
    }
}
