package com.serkodesign.tepera.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Перемикач мови застосунку (Налаштування → Мова застосунку).
 *
 * ВАЖЛИВО: `AppCompatDelegate.setApplicationLocales()` (перша реалізація) НЕ спрацьовував —
 * та бібліотека застосовує збережену мову до ресурсів лише для `AppCompatActivity` (через
 * власний `attachBaseContext`-хук у `AppCompatDelegateImpl`); `MainActivity` — звичайний
 * `ComponentActivity`, тож виклик просто ЗАПАМ'ЯТОВУВАВ вибір (`getApplicationLocales()`
 * коректно повертав його), але жоден реальний ресурс/рядок так і не перемикався — підтверджено
 * на Samsung S23 (Android 16): вибір "English" лишався позначеним, а UI лишався українською.
 *
 * Фікс — ручне обгортання контексту: SharedPreferences (не DataStore — читання має бути
 * СИНХРОННИМ у attachBaseContext(), до того, як з'явиться Coroutine-інфраструктура) зберігає
 * тег мови, `wrap()` викликається з `MainActivity.attachBaseContext()`, а після зміни вибору —
 * `Activity.recreate()` застосовує нову мову негайно. Працює однаково на всіх API-рівнях
 * (26+), на відміну від системного per-app language (LocaleManager, лише API 33+).
 */
object LocaleStore {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    /** Порожній рядок = "Системна" (мова пристрою, без перевизначення). */
    fun getLanguageTag(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, "") ?: ""

    fun setLanguageTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, tag)
            .apply()
    }

    /** Викликається з MainActivity.attachBaseContext() — до появи будь-якого Compose UI. */
    fun wrap(context: Context): Context {
        val tag = getLanguageTag(context)
        if (tag.isEmpty()) return context

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
