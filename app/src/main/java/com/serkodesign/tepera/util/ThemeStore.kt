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
     * Вибір теми в Налаштуваннях → Загальні → Тема. Увімкнено за запитом власника. За замовчуванням — системна
     * (слідує за темою телефона). false — приховує вибір і робить застосунок завжди світлим (SRS FR-6.7 v4.2).
     */
    const val THEME_CHOICE_ENABLED = true

    enum class Mode { SYSTEM, LIGHT, DARK }

    /** За замовчуванням — системна тема: слідує за темою телефона, як вирішив власник. */
    private val defaultMode = Mode.SYSTEM
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
