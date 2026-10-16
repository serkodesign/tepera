package com.serkodesign.tepera.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Тумблер "Додаткові анімації" (Налаштування → Загальні): рідина в орбі (хвилі, нахил, сплески), числа, що
 * докручуються, плавні переходи кнопок і підсвітка збереженого запису. Базові переходи між екранами не чіпає.
 * SharedPreferences (синхронне читання, як [ThemeStore]) + `mutableStateOf` — зміна діє одразу. За замовчуванням — увімкнено.
 */
object MotionStore {
    private const val PREFS_NAME = "motion_prefs"
    private const val KEY_EXTRA = "extra_animations"

    private val state = mutableStateOf(true)
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        state.value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_EXTRA, true)
    }

    /** Читання в композиції перемальовує її при зміні. */
    fun extraAnimations(): Boolean = state.value

    fun setExtraAnimations(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_EXTRA, enabled).apply()
        state.value = enabled
    }
}
