package com.serkodesign.tepera.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * «Настрій застосунку» в Налаштуваннях → Загальні: "Поточний" (класичний вигляд) або "Світанок" (небо за порою доби,
 * скляні картки, персиковий акцент). Незалежний від теми Світла/Темна. SharedPreferences (синхронне читання, як
 * [ThemeStore]) + `mutableStateOf`, тож зміна перемальовує застосунок без перезапуску.
 */
object MoodStore {
    private const val PREFS_NAME = "mood_prefs"
    private const val KEY_MOOD = "app_mood"

    enum class Mood { CLASSIC, DAWN }

    private val defaultMood = Mood.CLASSIC
    private val state = mutableStateOf(defaultMood)
    private var loaded = false

    /** Читає збережений вибір один раз; безпечно викликати з кожної композиції. */
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_MOOD, defaultMood.name)
        state.value = runCatching { Mood.valueOf(stored ?: defaultMood.name) }.getOrDefault(defaultMood)
    }

    /** Поточний вибір; читання в композиції перемальовує її при зміні. */
    fun current(): Mood = state.value

    fun set(context: Context, mood: Mood) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_MOOD, mood.name).apply()
        state.value = mood
    }
}
