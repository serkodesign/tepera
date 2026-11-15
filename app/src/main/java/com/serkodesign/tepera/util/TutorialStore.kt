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

    private const val MIGRATED = "_migrated_122"

    /**
     * 1.2.2: підказки лише для чистої установки й оновлення з 1.0.5 і раніше. Хто оновився з 1.1.x/1.2.1, уже мав
     * `whats_new_121_seen` (його ставить вікно «Що змінилось» 1.2.1) — для нього все позначається переглянутим.
     * Виконується один раз, до першого читання підказок.
     */
    fun migrateIfNeeded(context: Context, cameFromV12: Boolean) {
        runCatching {
            val p = prefs(context)
            if (p.getBoolean(MIGRATED, false)) return
            val e = p.edit().putBoolean(MIGRATED, true)
            if (cameFromV12) listOf(HOME_CHAIN, DIARY_HINT, SETTINGS_HINT).forEach { e.putBoolean(it, true) }
            e.apply()
        }
    }

    /** «Показати підказки знову»: все скидається. */
    fun reset(context: Context) {
        runCatching { prefs(context).edit().clear().apply() }
    }
}
