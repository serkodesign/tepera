package com.serkodesign.tepera.analytics

import android.content.Context

/** Згода на віддалену аналітику (рівень 2). За замовчуванням ВИМКНЕНО (opt-in): без явного «Так» нічого не йде в мережу. */
interface AnalyticsConsent {
    fun isEnabled(): Boolean
}

/**
 * Тонка обгортка над SDK віддаленої аналітики — щоб логіку фільтрації можна було тестувати без SDK й без пристрою.
 * Реалізація ([PostHogRemoteClient]) нічого не знає про категорії подій чи згоду.
 */
interface RemoteClient {
    fun capture(name: String, props: Map<String, Any>)
    fun setOptOut(optOut: Boolean)
    /** Анонімний випадковий ID аналітики, який показуємо в налаштуваннях (для запиту на видалення). */
    fun distinctId(): String
    /** Новий випадковий ID; старі події з ним лишаються непов'язаними з новими. */
    fun reset()
}

/**
 * Віддалений отримувач подій. Пропускає лише:
 *  — коли клієнт існує (ключ проєкту заданий) і згода надана;
 *  — категорію A (категорія B — похідна від Accessibility/UsageStats — лише якщо [allowCategoryB], рішення D4 = «тільки локально»);
 *  — властивості з allowlist ([AnalyticsAllowlist]) + службова `app_version`.
 */
class RemoteAnalytics(
    private val client: RemoteClient?,
    private val consent: AnalyticsConsent,
    private val appVersion: String,
    private val allowCategoryB: Boolean = false
) : Analytics {
    override fun track(event: AnalyticsEvent) {
        val client = client ?: return
        if (!consent.isEnabled()) return
        if (event.category == Category.B_SYSTEM_DERIVED && !allowCategoryB) return
        val props = AnalyticsAllowlist.sanitize(event.props) + ("app_version" to appVersion)
        client.capture(event.name, props)
    }
}

/** Згода в SharedPreferences (синхронне читання з будь-якого потоку; `decided` — чи людина вже бачила вибір). */
class AnalyticsConsentStore(context: Context) : AnalyticsConsent {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    /** Людина вже зробила вибір («Так» чи «Ні, дякую»): повторно не питаємо. */
    fun isDecided(): Boolean = prefs.getBoolean(KEY_DECIDED, false)

    fun set(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).putBoolean(KEY_DECIDED, true)
            .apply { if (enabled) putBoolean(KEY_EVER_ENABLED, true) }.apply()
    }

    /** Чи аналітику коли-небудь вмикали (тоді «Видалити мої дані» лишається доступним і після вимкнення). */
    fun wasEverEnabled(): Boolean = prefs.getBoolean(KEY_EVER_ENABLED, false)

    /** Скільки разів уже показано діалог (локально; для оцінки частки згод). */
    fun shownCount(): Int = prefs.getInt(KEY_SHOWN, 0)

    /** Чи людина вже раз відклала вибір («Не зараз»): другий «Не зараз» вважається відмовою. */
    fun postponedOnce(): Boolean = prefs.getInt(KEY_POSTPONED, 0) >= 1

    fun markShown() {
        prefs.edit().putInt(KEY_SHOWN, shownCount() + 1).apply()
    }

    /** «Не зараз»: нічого не вмикається, питаємо знову не раніше ніж через [SNOOZE_DAYS] днів. */
    fun postpone(nowMillis: Long) {
        prefs.edit()
            .putInt(KEY_POSTPONED, prefs.getInt(KEY_POSTPONED, 0) + 1)
            .putLong(KEY_SNOOZE_UNTIL, nowMillis + SNOOZE_DAYS * DAY_MS)
            .apply()
    }

    /** Чи настав час питати: вибір не зроблено й відкладення (якщо було) спливло. */
    fun isDue(nowMillis: Long): Boolean = !isDecided() && nowMillis >= prefs.getLong(KEY_SNOOZE_UNTIL, 0L)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "analytics_consent"
        const val KEY_ENABLED = "enabled"
        const val KEY_DECIDED = "decided"
        const val KEY_EVER_ENABLED = "ever_enabled"
        const val KEY_SHOWN = "shown_count"
        const val KEY_POSTPONED = "postponed_count"
        const val KEY_SNOOZE_UNTIL = "snooze_until"
        const val SNOOZE_DAYS = 30L
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
