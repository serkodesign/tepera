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

private const val DEFAULT_TARGET_MINUTES = 180 // FR-3.4

/** FR-3.4 (таргет) і FR-7.1 (чи вже показаний онбординг доступу до статистики). */
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
}
