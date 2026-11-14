package com.serkodesign.tepera.util

import android.content.Context

/**
 * Які підказки туторіалу людина вже бачила. SharedPreferences (синхронно, без міграцій): це лише «показано/ні», не дані
 * користувача. Ланцюжок на Головній + разові контекстні підказки на Щоденнику й Налаштуваннях.
 */
object TutorialStore {
    const val HOME_CHAIN = "home_chain"
    const val DIARY_HINT = "diary_hint"
    const val SETTINGS_HINT = "settings_hint"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("tutorial", Context.MODE_PRIVATE)

    fun isSeen(context: Context, key: String): Boolean = runCatching { prefs(context).getBoolean(key, false) }.getOrDefault(true)

    fun markSeen(context: Context, key: String) {
        runCatching { prefs(context).edit().putBoolean(key, true).apply() }
    }

    /** «Показати підказки знову»: все скидається. */
    fun reset(context: Context) {
        runCatching { prefs(context).edit().clear().apply() }
    }
}
