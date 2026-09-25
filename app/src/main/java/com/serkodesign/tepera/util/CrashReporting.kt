package com.serkodesign.tepera.util

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * D-15: звіти про збої (Firebase Crashlytics) — опційні. Стан лежить у SharedPreferences, а не в
 * DataStore: його треба прочитати синхронно в `Application.onCreate()`, до появи корутин.
 *
 * Збір за замовчуванням вимкнений у маніфесті (`firebase_crashlytics_collection_enabled=false`),
 * тож нічого не надсилається, доки [apply] явно не ввімкне його за збереженим вибором. На час
 * закритого тесту вибір за замовчуванням — УВІМКНЕНО ([DEFAULT_ENABLED]); користувачу про це
 * чесно кажемо на першому екрані онбордингу (`CrashReportsNotice`) і дозволяємо вимкнути там
 * само й у Налаштуваннях. Після тесту рішення про значення за замовчуванням переглянути.
 */
object CrashReporting {
    private const val PREFS_NAME = "privacy_prefs"
    private const val KEY_ENABLED = "crash_reports_enabled"
    private const val DEFAULT_ENABLED = true

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, DEFAULT_ENABLED)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        apply(context)
    }

    /** Застосовує збережений вибір до SDK: викликається щозапуску й після кожної зміни. */
    fun apply(context: Context) {
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isEnabled(context)) }
    }

    /** Повертає вибір до значення за замовчуванням ("Видалити всі дані"). */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        apply(context)
    }
}
