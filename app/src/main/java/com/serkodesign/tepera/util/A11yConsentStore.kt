package com.serkodesign.tepera.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Згода на розкриття використання AccessibilityService (екран розкриття, крок 3). SharedPreferences, бо службі
 * потрібне синхронне читання (як у [PlanOverlayStore]). Не потрапляє в Room і в експорт.
 */
object A11yConsentStore {
    private const val PREFS = "a11y_consent"
    const val KEY_ACCEPTED = "a11y_disclosure_accepted"
    const val KEY_ACCEPTED_AT = "a11y_disclosure_accepted_at"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isAccepted(context: Context): Boolean = prefs(context).getBoolean(KEY_ACCEPTED, false)

    fun accept(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        prefs(context).edit().putBoolean(KEY_ACCEPTED, true).putLong(KEY_ACCEPTED_AT, nowMillis).apply()
    }

    /** Слухач зміни згоди для служби (кешує значення). Тримати посилання, поки слухач потрібен. */
    fun listen(context: Context, onChanged: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ACCEPTED) onChanged()
        }
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun stopListening(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
}

/** Стан функцій, що потребують служби доступності. */
enum class A11yFeatureState {
    /** Flavor без служби. */
    Unavailable,
    /** Згоди на розкриття ще немає. */
    NeedsDisclosure,
    /** Згода є, але службу не ввімкнено в системі. */
    NeedsServiceEnabled,
    Active;

    companion object {
        /** Чиста логіка стану (тестується без Android). */
        fun compute(available: Boolean, accepted: Boolean, serviceEnabled: Boolean): A11yFeatureState = when {
            !available -> Unavailable
            !accepted -> NeedsDisclosure
            !serviceEnabled -> NeedsServiceEnabled
            else -> Active
        }

        fun current(context: Context): A11yFeatureState = compute(
            available = GateAccessibility.isAvailable,
            accepted = A11yConsentStore.isAccepted(context),
            serviceEnabled = GateAccessibility.isEnabled(context),
        )
    }
}
