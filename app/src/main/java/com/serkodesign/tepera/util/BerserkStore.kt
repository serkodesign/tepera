package com.serkodesign.tepera.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Берсерк-режим (лише flavor beta, де є служба доступності): налаштування й блокування. SharedPreferences, бо читається
 * зі служби синхронно, а це не дані користувача (у Room і в експорт не входить).
 *
 * Спосіб підрахунку часу ([CountMode]) стосується і берсерка, і «Відкрити на 5 хв» (м'який вихід).
 */
object BerserkStore {
    /** Як рахувати час у стрічках: окремо для кожного застосунку чи разом для всіх застосунків-стрічок. */
    enum class CountMode { PER_APP, ALL }

    val HOUR_OPTIONS = listOf(30, 45, 60, 90, 120)
    val BLOCK_OPTIONS = listOf(15, 30, 60, 120, 180)

    /** Перерва в стрічках довша за це обнуляє відлік. Не в меню: інакше довелось би пояснювати алгоритм. */
    const val BREAK_MILLIS = 3 * 60_000L

    private const val PREFS = "berserk"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour_minutes"
    private const val KEY_BLOCK_LEN = "block_len_minutes"
    private const val KEY_MODE = "count_mode"
    private const val KEY_SKIP_UNTIL = "skip_until"
    private const val KEY_BLOCK_ALL = "blocked_all"
    private const val KEY_BLOCK_PREFIX = "blocked_pkg_"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** За замовчуванням вимкнено: жорсткий блок не має заставати людину зненацька. */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        val p = prefs(context)
        val edit = p.edit().putBoolean(KEY_ENABLED, enabled)
        // Вимкнули — чинні блокування знімаються: інакше вони висіли б невидимо до наступного вмикання.
        if (!enabled) clearBlocks(edit, p)
        edit.apply()
    }

    fun hourMinutes(context: Context): Int = prefs(context).getInt(KEY_HOUR, 60)

    fun setHourMinutes(context: Context, minutes: Int) = prefs(context).edit().putInt(KEY_HOUR, minutes).apply()

    fun blockMinutes(context: Context): Int = prefs(context).getInt(KEY_BLOCK_LEN, 60)

    fun setBlockMinutes(context: Context, minutes: Int) = prefs(context).edit().putInt(KEY_BLOCK_LEN, minutes).apply()

    /** За замовчуванням — окремо для кожного: так працювало й до появи вибору. */
    fun countMode(context: Context): CountMode =
        runCatching { CountMode.valueOf(prefs(context).getString(KEY_MODE, null).orEmpty()) }.getOrDefault(CountMode.PER_APP)

    fun setCountMode(context: Context, mode: CountMode) = prefs(context).edit().putString(KEY_MODE, mode.name).apply()

    /** Ключ відліку: у спільному режимі один на всі стрічки. */
    fun trackingKey(mode: CountMode, packageName: String): String = if (mode == CountMode.ALL) "*" else packageName

    /** До якого моменту заблокована стрічка [packageName] (0 — не заблокована). */
    fun blockedUntil(context: Context, packageName: String): Long {
        val p = prefs(context)
        return if (countMode(context) == CountMode.ALL) p.getLong(KEY_BLOCK_ALL, 0L) else p.getLong(KEY_BLOCK_PREFIX + packageName, 0L)
    }

    fun setBlock(context: Context, packageName: String, untilMillis: Long) {
        val key = if (countMode(context) == CountMode.ALL) KEY_BLOCK_ALL else KEY_BLOCK_PREFIX + packageName
        prefs(context).edit().putLong(key, untilMillis).apply()
    }

    /** «Важлива справа»: берсерк вимкнений до кінця логічної доби (01:00), чинні блокування знімаються. */
    fun skipToday(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val p = prefs(context)
        val edit = p.edit().putLong(KEY_SKIP_UNTIL, nextDayRolloverMillis(nowMillis))
        clearBlocks(edit, p)
        edit.apply()
    }

    /**
     * Лише для екрана тестувальників: вмикає берсерк із ультракороткими інтервалами (година 1 хв, блок 2 хв), знімає
     * пропуск «на сьогодні» і чинні блокування, щоб увесь цикл можна було пройти за кілька хвилин.
     */
    fun applyTestValues(context: Context) {
        val p = prefs(context)
        val edit = p.edit().putBoolean(KEY_ENABLED, true).putInt(KEY_HOUR, 1).putInt(KEY_BLOCK_LEN, 2).remove(KEY_SKIP_UNTIL)
        clearBlocks(edit, p)
        edit.apply()
    }

    /** Повертає звичайні значення: вимкнено, година 60 хв, блок 60 хв, без пропуску й блокувань. */
    fun resetToDefaults(context: Context) {
        val p = prefs(context)
        val edit = p.edit().putBoolean(KEY_ENABLED, false).putInt(KEY_HOUR, 60).putInt(KEY_BLOCK_LEN, 60).remove(KEY_SKIP_UNTIL)
        clearBlocks(edit, p)
        edit.apply()
    }

    /** Чи зараз діє пропуск «на сьогодні». */
    fun isSkippedToday(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis < prefs(context).getLong(KEY_SKIP_UNTIL, 0L)

    private fun clearBlocks(edit: SharedPreferences.Editor, p: SharedPreferences) {
        p.all.keys.filter { it == KEY_BLOCK_ALL || it.startsWith(KEY_BLOCK_PREFIX) }.forEach { edit.remove(it) }
    }
}
