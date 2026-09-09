package com.serkodesign.tepera.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val TARGET_MINUTES_KEY = intPreferencesKey("target_minutes")
private val ONBOARDING_USAGE_ACCESS_SEEN_KEY = booleanPreferencesKey("onboarding_usage_access_seen")
private val SLEEP_WINDOW_END_HOUR_KEY = intPreferencesKey("sleep_window_end_hour")

private const val DEFAULT_TARGET_MINUTES = 180 // FR-3.10
private const val DEFAULT_SLEEP_WINDOW_END_HOUR = 6 // FR-3.2

/**
 * FR-3.10 (орієнтир Online-часу), FR-7.1 (чи вже показаний онбординг доступу до статистики)
 * і FR-3.2 (редагована межа "вікна сну" — коли починається відлік точки старту дня, SRS v2.5).
 */
class SettingsStore(private val context: Context) {

    val targetMinutes: Flow<Int> = context.settingsDataStore.data
        .map { it[TARGET_MINUTES_KEY] ?: DEFAULT_TARGET_MINUTES }

    suspend fun setTargetMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[TARGET_MINUTES_KEY] = minutes }
    }

    val onboardingUsageAccessSeen: Flow<Boolean> = context.settingsDataStore.data
        .map { it[ONBOARDING_USAGE_ACCESS_SEEN_KEY] ?: false }

    suspend fun setOnboardingUsageAccessSeen() {
        context.settingsDataStore.edit { it[ONBOARDING_USAGE_ACCESS_SEEN_KEY] = true }
    }

    /**
     * FR-3.2: година, якою закінчується "вікно сну" (дефолт 6 — 00:00–06:00). До цієї години
     * BalanceRepository.calculateDayStart() ігнорує розблокування коротші за 5 хв (нічна
     * перевірка годинника); редагується в Налаштуваннях для нічних змін/сов.
     */
    val sleepWindowEndHour: Flow<Int> = context.settingsDataStore.data
        .map { it[SLEEP_WINDOW_END_HOUR_KEY] ?: DEFAULT_SLEEP_WINDOW_END_HOUR }

    suspend fun setSleepWindowEndHour(hour: Int) {
        context.settingsDataStore.edit { it[SLEEP_WINDOW_END_HOUR_KEY] = hour.coerceIn(0, 11) }
    }
}
