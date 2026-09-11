package com.serkodesign.tepera.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val TARGET_MINUTES_KEY = intPreferencesKey("target_minutes")
private val ONBOARDING_USAGE_ACCESS_SEEN_KEY = booleanPreferencesKey("onboarding_usage_access_seen")
private val SLEEP_WINDOW_END_HOUR_KEY = intPreferencesKey("sleep_window_end_hour")
private val VALUES_ONBOARDING_SEEN_KEY = booleanPreferencesKey("values_onboarding_seen")
private val VALUED_CATEGORY_ID_KEY = stringPreferencesKey("valued_category_id")
private val LAST_REFLECTION_HANDLED_AT_KEY = longPreferencesKey("last_reflection_handled_at")
private val FIRST_LAUNCH_AT_KEY = longPreferencesKey("first_launch_at")
private val PATTERN_CARD_DISMISSED_KEY = longPreferencesKey("pattern_card_dismissed_key")
private val WEEKLY_DIGEST_CARD_DISMISSED_KEY = longPreferencesKey("weekly_digest_card_dismissed_key")
private val PAUSE_CARD_DISMISSED_KEY = longPreferencesKey("pause_card_dismissed_key")
private val LAST_PHONE_USE_CARD_DISMISSED_KEY = longPreferencesKey("last_phone_use_card_dismissed_key")

private const val DEFAULT_TARGET_MINUTES = 180 // FR-3.10
private const val DEFAULT_SLEEP_WINDOW_END_HOUR = 6 // FR-3.2
private const val REFLECTION_INTERVAL_MILLIS = 7 * 24 * 60 * 60 * 1000L // FR-P.1: раз на тиждень

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

    /**
     * FR-P.2: чи вже показане одноразове онбординг-питання про цінності ("Що ти хотів би
     * робити більше?"). Показується ЗАВЖДИ першим при першому запуску — HomeScreen перевіряє
     * цей прапорець РАНІШЕ за onboardingUsageAccessSeen (FR-7.1).
     */
    val valuesOnboardingSeen: Flow<Boolean> = context.settingsDataStore.data
        .map { it[VALUES_ONBOARDING_SEEN_KEY] ?: false }

    /** [categoryId] null, якщо користувач пропустив питання — це теж валідний вибір. */
    suspend fun setValuesOnboardingAnswer(categoryId: String?) {
        context.settingsDataStore.edit {
            it[VALUES_ONBOARDING_SEEN_KEY] = true
            if (categoryId != null) it[VALUED_CATEGORY_ID_KEY] = categoryId
        }
    }

    val valuedCategoryId: Flow<String?> = context.settingsDataStore.data
        .map { it[VALUED_CATEGORY_ID_KEY] }

    /**
     * FR-P.1: тижнева рефлексія "оцінка → реальність" — раз на 7 днів, необов'язково.
     * Перше значення НЕ 0/"ніколи" (це показало б картку одразу після встановлення, коли ще
     * нема тижня даних) — [seedIfUnset] виставляє точку відліку на момент першого запуску,
     * викликається з TeperaApp.onCreate() поруч з іншим одноразовим сідінгом.
     */
    val lastReflectionHandledAtMillis: Flow<Long> = context.settingsDataStore.data
        .map { it[LAST_REFLECTION_HANDLED_AT_KEY] ?: 0L }

    suspend fun setLastReflectionHandledAtMillis(millis: Long) {
        context.settingsDataStore.edit { it[LAST_REFLECTION_HANDLED_AT_KEY] = millis }
    }

    suspend fun seedLastReflectionHandledAtIfUnset() {
        val alreadySet = context.settingsDataStore.data.first()[LAST_REFLECTION_HANDLED_AT_KEY] != null
        if (!alreadySet) setLastReflectionHandledAtMillis(System.currentTimeMillis())
    }

    fun isReflectionDue(lastHandledMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis - lastHandledMillis >= REFLECTION_INTERVAL_MILLIS

    /**
     * FR-D.9: коли застосунок вперше запущено — поріг "тиждень даних" для теплового патерну
     * (FR-D.8/D.9, PatternViewModel). Окремий ключ від lastReflectionHandledAtMillis (та сама
     * ідея сідингу при першому запуску, але інша семантика — переплутати їх означало б, що
     * ручна відповідь/skip тижневої рефлексії випадково зсуває й поріг готовності патерну).
     */
    val firstLaunchMillis: Flow<Long> = context.settingsDataStore.data
        .map { it[FIRST_LAUNCH_AT_KEY] ?: 0L }

    suspend fun seedFirstLaunchMillisIfUnset() {
        val alreadySet = context.settingsDataStore.data.first()[FIRST_LAUNCH_AT_KEY] != null
        if (!alreadySet) context.settingsDataStore.edit { it[FIRST_LAUNCH_AT_KEY] = System.currentTimeMillis() }
    }

    /**
     * "Закрити картку, якщо прочитав" — за прямим запитом користувача (не в SRS). Кожна
     * контекстна картка (`PatternMiniCard`, `WeeklyDigestCard`, `PauseCard`) зберігає ОПАЧНИЙ
     * ключ "якого саме вікна даних вона стосувалась" при закритті (не просто timestamp) —
     * `PatternViewModel`/`WeeklyDigestViewModel` порівнюють з ключем сьогоднішнього вікна (доба
     * змінюється — картка повертається), `PauseViewModel` з якорем конкретного вікна опитування
     * (FR-D.3: сьогоднішній вечір і вчорашній ранок — різні дані, "закрито" не повинно ховати
     * ІНШЕ вікно). Значення -1L = ще ніколи не закривали.
     */
    val patternCardDismissedKey: Flow<Long> = context.settingsDataStore.data
        .map { it[PATTERN_CARD_DISMISSED_KEY] ?: -1L }

    suspend fun setPatternCardDismissedKey(key: Long) {
        context.settingsDataStore.edit { it[PATTERN_CARD_DISMISSED_KEY] = key }
    }

    val weeklyDigestCardDismissedKey: Flow<Long> = context.settingsDataStore.data
        .map { it[WEEKLY_DIGEST_CARD_DISMISSED_KEY] ?: -1L }

    suspend fun setWeeklyDigestCardDismissedKey(key: Long) {
        context.settingsDataStore.edit { it[WEEKLY_DIGEST_CARD_DISMISSED_KEY] = key }
    }

    val pauseCardDismissedKey: Flow<Long> = context.settingsDataStore.data
        .map { it[PAUSE_CARD_DISMISSED_KEY] ?: -1L }

    suspend fun setPauseCardDismissedKey(key: Long) {
        context.settingsDataStore.edit { it[PAUSE_CARD_DISMISSED_KEY] = key }
    }

    /**
     * FR-D.7 (SRS v2.8): ключ закриття картки "востаннє брав телефон о HH:MM" — тут це не
     * початок календарної доби (як у pattern/weeklyDigest), а межа 02:00-зсунутої "доби"
     * (FR-D.7a), та сама, що визначає, ЯКЕ "вчора" показує метрика. Закриття діє, доки ця
     * межа не зсунеться на наступну.
     */
    val lastPhoneUseCardDismissedKey: Flow<Long> = context.settingsDataStore.data
        .map { it[LAST_PHONE_USE_CARD_DISMISSED_KEY] ?: -1L }

    suspend fun setLastPhoneUseCardDismissedKey(key: Long) {
        context.settingsDataStore.edit { it[LAST_PHONE_USE_CARD_DISMISSED_KEY] = key }
    }
}
