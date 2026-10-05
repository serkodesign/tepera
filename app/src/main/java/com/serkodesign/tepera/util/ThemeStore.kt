package com.serkodesign.tepera.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Вибір теми в Налаштування → Загальні: "Системна" / "Світла" / "Темна". Зберігається в SharedPreferences
 * (синхронне читання, як у [LocaleStore]) — тема потрібна одразу при першому кадрі, до будь-яких корутин.
 * Стан у `mutableStateOf`, тож зміна перемальовує застосунок без перезапуску.
 */
object ThemeStore {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_MODE = "theme_mode"

    /**
     * Вибір теми в Налаштуваннях → Загальні → Тема приховано (за рішенням власника): застосунок завжди світлий.
     * Код вибору лишається — щоб повернути, достатньо поставити true. Коли false, [TeperaTheme] ігнорує
     * збережений режим і системну тему.
     */
    const val THEME_CHOICE_ENABLED = false

    enum class Mode { SYSTEM, LIGHT, DARK }

    /** За замовчуванням — світла тема (не системна). */
    private val defaultMode = Mode.LIGHT
    private val state = mutableStateOf(defaultMode)
    private var loaded = false

    /** Читає збережений вибір один раз; безпечно викликати з кожної композиції. */
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODE, defaultMode.name)
        state.value = runCatching { Mode.valueOf(stored ?: defaultMode.name) }.getOrDefault(defaultMode)
    }

    /** Поточний вибір; читання в композиції перемальовує її при зміні. */
    fun current(): Mode = state.value

    fun set(context: Context, mode: Mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
        state.value = mode
    }
}
