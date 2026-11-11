package com.serkodesign.tepera.util

import android.content.Context

/**
 * «Не сьогодні» на картці наступної години (Головна v2, Figma 1081:2169): вікно мовчить до кінця свого найближчого
 * проходу — без підказки поверх стрічки й без нагадування. Вимкнути можна лише зсередини Tepera (зі сповіщення чи листа
 * ні — щоб це не стало рефлексом). Зберігається момент, до якого вікно мовчить; минулий запис просто перестає діяти.
 */
object WindowSkipStore {
    private const val PREFS = "window_skip"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun skipUntil(context: Context, windowId: String, untilMillis: Long) {
        prefs(context).edit().putLong(windowId, untilMillis).apply()
    }

    fun isSkipped(context: Context, windowId: String, atMillis: Long = System.currentTimeMillis()): Boolean =
        prefs(context).getLong(windowId, 0L) > atMillis

    fun undo(context: Context, windowId: String) {
        prefs(context).edit().remove(windowId).apply()
    }
}
