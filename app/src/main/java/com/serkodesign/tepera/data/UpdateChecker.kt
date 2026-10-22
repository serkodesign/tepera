package com.serkodesign.tepera.data

import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.serkodesign.tepera.BuildConfig
import kotlinx.coroutines.tasks.await

/** Текст "що нового" (UA/EN) для діалогу оновлення; порожній рядок — використати стандартний. */
data class WhatsNew(val uk: String, val en: String)

/**
 * CC-9: м'яка пропозиція оновитись.
 *
 * ФАКТ наявності нової версії визначає Google Play In-App Updates API (гнучкий режим) — вручну
 * нічого оновлювати не треба. Працює лише для збірок, встановлених із Google Play: на APK, поставленому
 * напряму (beta, debug), Play повертає "оновлення нема", діалог не з'являється.
 *
 * Текст "що нового" Play не вміє, тому він необов'язково береться з Firebase Remote Config
 * (`whats_new_uk`/`whats_new_en`, той самий проєкт, що Crashlytics). Нема тексту в консолі — діалог
 * показує стандартний рядок.
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val KEY_WHATS_NEW_UK = "whats_new_uk"
    private const val KEY_WHATS_NEW_EN = "whats_new_en"

    /** `null` — запит до Play не вдався (мовчимо, не нав'язуємось). */
    suspend fun checkPlay(manager: AppUpdateManager): AppUpdateInfo? = runCatching {
        val info = manager.appUpdateInfo.await()
        Log.d(
            TAG,
            "Play: updateAvailability=${info.updateAvailability()} (1=нема,2=є,3=триває), " +
                "installStatus=${info.installStatus()}, availableVersionCode=${info.availableVersionCode()}, " +
                "flexibleAllowed=${info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)}, current=${BuildConfig.VERSION_CODE}"
        )
        info
    }.onFailure { Log.w(TAG, "Play In-App Updates check failed", it) }.getOrNull()

    /** Порожні рядки, якщо в Remote Config нічого нема або запит не вдався. */
    suspend fun fetchWhatsNew(): WhatsNew = runCatching {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else 3600L
            }
        ).await()
        remoteConfig.setDefaultsAsync(mapOf(KEY_WHATS_NEW_UK to "", KEY_WHATS_NEW_EN to "")).await()
        remoteConfig.fetchAndActivate().await()
        WhatsNew(
            uk = remoteConfig.getString(KEY_WHATS_NEW_UK),
            en = remoteConfig.getString(KEY_WHATS_NEW_EN)
        )
    }.onFailure { Log.w(TAG, "Remote Config fetch failed", it) }.getOrDefault(WhatsNew("", ""))
}
