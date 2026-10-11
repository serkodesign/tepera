package com.serkodesign.tepera.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.serkodesign.tepera.BuildConfig
import kotlinx.coroutines.tasks.await

/** CC-9: дані для м'якої пропозиції оновитись — версія з Play Store і текст "що нового" (UA/EN). */
data class UpdateInfo(val versionCode: Long, val whatsNewUk: String, val whatsNewEn: String)

/**
 * CC-9: Firebase Remote Config (той самий проєкт, що Crashlytics) — єдиний спосіб повідомити вже
 * встановлений застосунок про ВМІСТ оновлення без власного бекенду. Google Play In-App Update API
 * уміє визначити лише сам ФАКТ наявності новішої версії, без довільного тексту; тут текст важливий
 * ("повідомлення про те, що додалось"), тож джерело — Remote Config, ключі оновлюються вручну при
 * кожному релізі (той самий крок, що вже існує для docs/CHANGELOG-*.md).
 *
 * Дефолт `latest_version_code` = поточний [BuildConfig.VERSION_CODE] застосунку, тож доки ніхто не
 * підняв значення в консолі Firebase, діалог НІКОЛИ не з'являється — безпечно за замовчуванням.
 */
object UpdateChecker {
    private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    private const val KEY_WHATS_NEW_UK = "whats_new_uk"
    private const val KEY_WHATS_NEW_EN = "whats_new_en"

    /** `null` — немає новішої версії (або запит не вдався; мовчимо, не нав'язуємось). */
    suspend fun fetchLatest(context: Context): UpdateInfo? = runCatching {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else 3600L
            }
        ).await()
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_LATEST_VERSION_CODE to BuildConfig.VERSION_CODE.toLong(),
                KEY_WHATS_NEW_UK to "",
                KEY_WHATS_NEW_EN to ""
            )
        ).await()
        remoteConfig.fetchAndActivate().await()
        val latest = remoteConfig.getLong(KEY_LATEST_VERSION_CODE)
        if (latest <= BuildConfig.VERSION_CODE) return null
        UpdateInfo(
            versionCode = latest,
            whatsNewUk = remoteConfig.getString(KEY_WHATS_NEW_UK),
            whatsNewEn = remoteConfig.getString(KEY_WHATS_NEW_EN)
        )
    }.getOrNull()
}
