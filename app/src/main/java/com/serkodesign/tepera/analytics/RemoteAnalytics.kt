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
        prefs.edit().putBoolean(KEY_ENABLED, enabled).putBoolean(KEY_DECIDED, true).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "analytics_consent"
        const val KEY_ENABLED = "enabled"
        const val KEY_DECIDED = "decided"
    }
}
