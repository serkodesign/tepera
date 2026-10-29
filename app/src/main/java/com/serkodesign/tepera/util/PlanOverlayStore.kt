package com.serkodesign.tepera.util

import android.content.Context
import com.serkodesign.tepera.domain.risk.ActivitySuggestions

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
    private const val KEY_DECLINED = "declined"
    private const val KEY_LAST_PREFIX = "last_"
    private const val KEY_PENDING_PACKAGE = "pending_pkg"
    private const val KEY_PENDING_LABEL = "pending_label"
    private const val KEY_PENDING_THEN = "pending_then"
    private const val KEY_PENDING_AT = "pending_at"
    private const val KEY_SE_STARTED = "se_started"
    private const val KEY_SE_ON_TIME = "se_on_time"
    private const val KEY_SE_EXTENDED = "se_extended"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** За замовчуванням увімкнено: це суть режиму тесту; сервіс доступності й так потребує окремого дозволу. */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun lastShownMillis(context: Context, windowId: String): Long = prefs(context).getLong(KEY_LAST_PREFIX + windowId, 0L)

    fun recordShown(context: Context, windowId: String, nowMillis: Long = System.currentTimeMillis()) {
        val p = prefs(context)
        p.edit().putLong(KEY_LAST_PREFIX + windowId, nowMillis).putInt(KEY_SHOWN, p.getInt(KEY_SHOWN, 0) + 1).apply()
    }

    /** Підказка, від якої людина відмовилась («Інша ідея», «Відкрити на 5 хв», «Не сьогодні»): кілька днів її не повторюємо. */
    fun recordDeclined(context: Context, id: ActivitySuggestions.Id, nowMillis: Long = System.currentTimeMillis()) {
        val keep = nowMillis - ActivitySuggestions.DECLINE_DAYS * 24L * 3_600_000L
        val items = (declined(context).filterValues { it >= keep } + (id to nowMillis)).entries.joinToString(",") { "${it.key.name}:${it.value}" }
        prefs(context).edit().putString(KEY_DECLINED, items).apply()
    }

    fun declined(context: Context): Map<ActivitySuggestions.Id, Long> =
        prefs(context).getString(KEY_DECLINED, "").orEmpty().split(",").mapNotNull { part ->
            val (name, at) = part.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val id = ActivitySuggestions.Id.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
            at.toLongOrNull()?.let { id to it }
        }.toMap()

    fun recordContinued(context: Context) = bump(context, KEY_CONTINUED)

    fun recordLeft(context: Context) = bump(context, KEY_LEFT)

    /** Сесія «Відкрити на 5 хв», яку тільки-но започаткував лист плану; сервіс забирає її одразу, як лише вона записана. */
    data class PendingSoftExit(val packageName: String, val appLabel: String, val thenText: String, val startedAtMillis: Long)

    fun setPendingSoftExit(context: Context, session: PendingSoftExit) {
        prefs(context).edit()
            .putString(KEY_PENDING_PACKAGE, session.packageName).putString(KEY_PENDING_LABEL, session.appLabel)
            .putString(KEY_PENDING_THEN, session.thenText).putLong(KEY_PENDING_AT, session.startedAtMillis).apply()
    }

    /** Слухач запису сесії «Відкрити на 5 хв»: сервіс не мусить чекати на подію застосунку, яка не завжди приходить. Тримати посилання, поки слухач потрібен. */
    fun listenForPendingSoftExit(context: Context, onPending: () -> Unit): android.content.SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == KEY_PENDING_PACKAGE) onPending() }
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun stopListening(context: Context, listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)

    /** Забирає сесію незалежно від пакета (одноразово). Старша за хвилину — застаріла, ігнорується. */
    fun takeAnyPendingSoftExit(context: Context, nowMillis: Long = System.currentTimeMillis()): PendingSoftExit? {
        val pkg = prefs(context).getString(KEY_PENDING_PACKAGE, null) ?: return null
        return takePendingSoftExit(context, pkg, nowMillis)
    }

    /** Забирає сесію для [forPackage] (одноразово); для іншого пакета нічого не чіпає. Старша за хвилину — застаріла, ігнорується. */
    fun takePendingSoftExit(context: Context, forPackage: String, nowMillis: Long = System.currentTimeMillis()): PendingSoftExit? {
        val p = prefs(context)
        val pkg = p.getString(KEY_PENDING_PACKAGE, null) ?: return null
        if (pkg != forPackage) return null
        val at = p.getLong(KEY_PENDING_AT, 0L)
        val session = PendingSoftExit(pkg, p.getString(KEY_PENDING_LABEL, pkg).orEmpty(), p.getString(KEY_PENDING_THEN, "").orEmpty(), at)
        p.edit().remove(KEY_PENDING_PACKAGE).apply()
        return if (nowMillis - at <= 60_000L) session else null
    }

    /** М'який вихід: сесію розпочато / завершено вчасно (до кінця строку + 2 хв) / продовжено кнопкою «Ще 5». Час — для вікна «7 днів». */
    fun recordSoftExitStarted(context: Context, nowMillis: Long = System.currentTimeMillis()) = appendTime(context, KEY_SE_STARTED, nowMillis)

    fun recordSoftExitOnTime(context: Context, nowMillis: Long = System.currentTimeMillis()) = appendTime(context, KEY_SE_ON_TIME, nowMillis)

    fun recordSoftExitExtended(context: Context, nowMillis: Long = System.currentTimeMillis()) = appendTime(context, KEY_SE_EXTENDED, nowMillis)

    /** Скільки сесій розпочато / завершено вчасно / продовжено за останні [days] діб. */
    fun softExitCounts(context: Context, days: Int = 7, nowMillis: Long = System.currentTimeMillis()): Triple<Int, Int, Int> {
        val from = nowMillis - days * 24L * 3_600_000L
        return Triple(countSince(context, KEY_SE_STARTED, from), countSince(context, KEY_SE_ON_TIME, from), countSince(context, KEY_SE_EXTENDED, from))
    }

    private fun appendTime(context: Context, key: String, nowMillis: Long) {
        val keep = nowMillis - 31 * 24L * 3_600_000L
        val items = (readTimes(context, key).filter { it >= keep } + nowMillis).joinToString(",")
        prefs(context).edit().putString(key, items).apply()
    }

    private fun readTimes(context: Context, key: String): List<Long> =
        prefs(context).getString(key, "").orEmpty().split(",").mapNotNull { it.toLongOrNull() }

    private fun countSince(context: Context, key: String, from: Long) = readTimes(context, key).count { it >= from }

    fun shownCount(context: Context): Int = prefs(context).getInt(KEY_SHOWN, 0)
    fun continuedCount(context: Context): Int = prefs(context).getInt(KEY_CONTINUED, 0)
    fun leftCount(context: Context): Int = prefs(context).getInt(KEY_LEFT, 0)

    private fun bump(context: Context, key: String) {
        val p = prefs(context)
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
    }
}
