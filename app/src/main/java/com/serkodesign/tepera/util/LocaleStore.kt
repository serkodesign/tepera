package com.serkodesign.tepera.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

/**
 * Мова, що спостерігається з Compose: змінюється миттєво при виборі в Налаштуваннях, без
 * `Activity.recreate()` (перезапуск Activity обривав анімацію перемикача посеред руху й давав
 * системне мигання/ривок — скарга користувача). `MainActivity` обгортає весь UI в
 * [ProvideAppLocale], який підміняє `LocalContext`/`LocalConfiguration` на контекст із потрібною
 * локаллю, тож усі `stringResource()` перекомпоновуються на місці.
 */
object AppLocale {
    private val state = mutableStateOf<String?>(null)

    /** Поточний тег ("" = системна) — читається як Compose-state, тож викликач перекомпоновується. */
    fun tag(context: Context): String = state.value ?: LocaleStore.getLanguageTag(context).also { state.value = it }

    fun set(context: Context, tag: String) {
        LocaleStore.setLanguageTag(context, tag)
        Locale.setDefault(localeFor(tag))
        state.value = tag
    }

    fun localeFor(tag: String): Locale =
        if (tag.isEmpty()) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)

    /** Контекст-обгортка над [base] (Activity лишається досяжною через baseContext) з ресурсами потрібної локалі. */
    fun localizedContext(base: Context, tag: String): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(localeFor(tag))
        return LocalizedContext(base, base.createConfigurationContext(config).resources)
    }

    private class LocalizedContext(base: Context, private val localizedResources: Resources) : ContextWrapper(base) {
        override fun getResources(): Resources = localizedResources
    }
}

@Composable
fun ProvideAppLocale(content: @Composable () -> Unit) {
    val base = LocalContext.current
    val tag = AppLocale.tag(base)
    val localized = remember(tag, base) { AppLocale.localizedContext(base, tag) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content
    )
}

/** Знаходить Activity за ланцюжком ContextWrapper (LocalContext тепер — обгортка, не сама Activity). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
