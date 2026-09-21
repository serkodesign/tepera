package com.serkodesign.tepera.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.serkodesign.tepera.data.GapSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val TARGET_MINUTES_KEY = intPreferencesKey("target_minutes")
private val ONBOARDING_USAGE_ACCESS_SEEN_KEY = booleanPreferencesKey("onboarding_usage_access_seen")
private val VALUES_ONBOARDING_SEEN_KEY = booleanPreferencesKey("values_onboarding_seen")
private val VALUED_CATEGORY_ID_KEY = stringPreferencesKey("valued_category_id")
private val FIRST_LAUNCH_AT_KEY = longPreferencesKey("first_launch_at")
private val PATTERN_CARD_DISMISSED_KEY = longPreferencesKey("pattern_card_dismissed_key")
private val WEEKLY_DIGEST_CARD_DISMISSED_KEY = longPreferencesKey("weekly_digest_card_dismissed_key")
private val PAUSE_CARD_DISMISSED_KEY = longPreferencesKey("pause_card_dismissed_key")
private val ONLINE_ESTIMATE_ONBOARDING_SEEN_KEY = booleanPreferencesKey("online_estimate_onboarding_seen")
private val CATEGORY_ONBOARDING_SEEN_KEY = booleanPreferencesKey("category_onboarding_seen")
private val ONLINE_ESTIMATE_REVEAL_DISMISSED_ID_KEY = stringPreferencesKey("online_estimate_reveal_dismissed_id")
private val GAP_SENSITIVITY_KEY = stringPreferencesKey("gap_sensitivity")
private val HISTORY_BACKFILL_COMPLETED_AT_KEY = longPreferencesKey("history_backfill_completed_at")
private val GATES_PAUSED_UNTIL_KEY = longPreferencesKey("gates_paused_until")
private val CARD_EVENT_DISPLACEMENT_STREAK_KEY = intPreferencesKey("card_event_displacement_streak")
private val WIDGET_SUGGESTION_SEEN_KEY = booleanPreferencesKey("widget_suggestion_seen")
private val NOTIFICATION_PERMISSION_REQUESTED_KEY = booleanPreferencesKey("notification_permission_requested")
private val WIDGET_CATEGORY_IDS_KEY = stringPreferencesKey("widget_category_ids")

private const val DEFAULT_TARGET_MINUTES = 180 // FR-3.10

/**
 * FR-3.10 (орієнтир Online-часу), FR-7.1 (чи вже показаний онбординг доступу до статистики).
 * Вікно сну (T-12, tepera-dev-spec.md) — окремо в `SleepWindowRepository`/`sleep_windows`
 * (Room, не DataStore — до 2 вікон, кожне зі своїм ввімкненням, це вже не один скаляр).
 */
class SettingsStore(private val context: Context) {

    /** Повне очищення (Резервне копіювання → Видалити всі дані): усі ключі, включно з прапорцями онбордингу. */
    suspend fun clearAll() {
        context.settingsDataStore.edit { it.clear() }
    }

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
     * T-8 (tepera-dev-spec.md): чи вже показаний одноразовий вибір категорій на онбордингу
     * (`CategoryOnboardingScreen`) — між питанням про цінності (FR-P.2) і онбординг-оцінкою
     * Online-часу (T-3), той самий принцип "показано" фіксується одразу при відкритті екрана.
     */
    val categoryOnboardingSeen: Flow<Boolean> = context.settingsDataStore.data
        .map { it[CATEGORY_ONBOARDING_SEEN_KEY] ?: false }

    suspend fun setCategoryOnboardingSeen() {
        context.settingsDataStore.edit { it[CATEGORY_ONBOARDING_SEEN_KEY] = true }
    }

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
     * Figma user-flow (k6s4prQ9oK9x2uUvzHRghR, node 14:791): "Пропозиція віджета" — останній
     * крок онбордингу, показується РІВНО раз, ОБОМА гілками "доступ надано?" (так/ні) — HomeScreen
     * вирішує, коли саме він "розв'язаний" (див. LaunchedEffect у HomeScreen.kt), тут лише прапорець.
     */
    val widgetSuggestionSeen: Flow<Boolean> = context.settingsDataStore.data
        .map { it[WIDGET_SUGGESTION_SEEN_KEY] ?: false }

    suspend fun setWidgetSuggestionSeen() {
        context.settingsDataStore.edit { it[WIDGET_SUGGESTION_SEEN_KEY] = true }
    }

    /**
     * Обрані користувачем категорії кнопок віджета — у порядку кнопок (за прямим запитом
     * користувача). Порожній список = "не налаштовано": віджет лишається на автоматичному
     * сортуванні за порою доби (FR-4.5, `sortCategoriesForWidget`). Не Room — це налаштування, не
     * дані, міграція БД не потрібна; id зберігаються одним рядком через кому (UUID не містять ",").
     */
    val widgetCategoryIds: Flow<List<String>> = context.settingsDataStore.data
        .map { prefs -> prefs[WIDGET_CATEGORY_IDS_KEY]?.split(",")?.filter { it.isNotBlank() } ?: emptyList() }

    suspend fun setWidgetCategoryIds(ids: List<String>) {
        context.settingsDataStore.edit { it[WIDGET_CATEGORY_IDS_KEY] = ids.joinToString(",") }
    }

    /**
     * `POST_NOTIFICATIONS` (Android 13+) — потрібен для сповіщення "усе ще цим займаєшся?"
     * (`TimerCheckWorker`). Запитується РІВНО раз (HomeScreen) незалежно від відповіді
     * користувача — системний діалог і так не з'явиться вдруге після відмови без цього
     * прапорця, він лише запобігає повторному виклику `launch()` при кожному відкритті Home.
     */
    val notificationPermissionRequested: Flow<Boolean> = context.settingsDataStore.data
        .map { it[NOTIFICATION_PERMISSION_REQUESTED_KEY] ?: false }

    suspend fun setNotificationPermissionRequested() {
        context.settingsDataStore.edit { it[NOTIFICATION_PERMISSION_REQUESTED_KEY] = true }
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
     * T-3 (tepera-dev-spec.md): чи вже показане одноразове онбординг-питання "Скільки, по-твоєму,
     * ти був онлайн учора?" — між питанням про цінності (FR-P.2) і поясненням дозволу (FR-7.1),
     * той самий принцип "показано" фіксується одразу при відкритті екрана, незалежно від вибору
     * діапазону чи "Пропустити".
     */
    val onlineEstimateOnboardingSeen: Flow<Boolean> = context.settingsDataStore.data
        .map { it[ONLINE_ESTIMATE_ONBOARDING_SEEN_KEY] ?: false }

    suspend fun setOnlineEstimateOnboardingSeen() {
        context.settingsDataStore.edit { it[ONLINE_ESTIMATE_ONBOARDING_SEEN_KEY] = true }
    }

    /**
     * T-3: ключ закриття картки-розриву "твоя оцінка / насправді" — id конкретного
     * `UserEstimateEntity`, той самий принцип, що інші dismissed-ключі вище (опачний ідентифікатор
     * КОНКРЕТНОГО вікна даних, не просто timestamp). null = ще ніколи не закривали.
     */
    val onlineEstimateRevealDismissedId: Flow<String?> = context.settingsDataStore.data
        .map { it[ONLINE_ESTIMATE_REVEAL_DISMISSED_ID_KEY] }

    suspend fun setOnlineEstimateRevealDismissedId(id: String) {
        context.settingsDataStore.edit { it[ONLINE_ESTIMATE_REVEAL_DISMISSED_ID_KEY] = id }
    }

    /**
     * T-11 (tepera-dev-spec.md): пресет чутливості детекції пауз — Рідше/Звичайно/Частіше, без
     * числових полів (`GapDetectionConfig.forSensitivity()` перекладає пресет у чинні пороги).
     * Читається наживо при кожному скануванні (`PauseViewModel.refresh()`), тож зміна пресету
     * діє з наступного відкриття Home, без перезапуску застосунку.
     */
    val gapSensitivity: Flow<GapSensitivity> = context.settingsDataStore.data
        .map { prefs ->
            prefs[GAP_SENSITIVITY_KEY]?.let { runCatching { GapSensitivity.valueOf(it) }.getOrNull() }
                ?: GapSensitivity.NORMAL
        }

    suspend fun setGapSensitivity(sensitivity: GapSensitivity) {
        context.settingsDataStore.edit { it[GAP_SENSITIVITY_KEY] = sensitivity.name }
    }

    /**
     * T-2 (tepera-dev-spec.md): чи вже виконано одноразовий бекфіл історії пауз одразу після
     * надання доступу до статистики (`BackfillViewModel`) — 0L = ще ні. Незалежний від
     * [firstLaunchMillis]: бекфіл прив'язаний до моменту НАДАННЯ ДОЗВОЛУ, не запуску застосунку
     * (дозвіл часто надається пізніше за перший запуск, через онбординг-крок 3/4).
     */
    val historyBackfillCompletedAt: Flow<Long> = context.settingsDataStore.data
        .map { it[HISTORY_BACKFILL_COMPLETED_AT_KEY] ?: 0L }

    suspend fun setHistoryBackfillCompletedAt(millis: Long) {
        context.settingsDataStore.edit { it[HISTORY_BACKFILL_COMPLETED_AT_KEY] = millis }
    }

    /**
     * T-4 (tepera-dev-spec.md): "тимчасове вимкнення воріт на день" — один тап, без підтвердження
     * (розділ 2.3 документа "автономія важливіша за ефективність"). Зберігає момент, ДО якого
     * ворота призупинені (кінець поточної календарної доби, рахує викликач) — 0L = не призупинено.
     * T-5 (майбутня сесія, екран паузи) звірятиме `System.currentTimeMillis() < gatesPausedUntilMillis`
     * перед показом паузи; сам перемикач і UI — тут, у T-4, за буквальною вимогою списку "Зробити".
     */
    val gatesPausedUntilMillis: Flow<Long> = context.settingsDataStore.data
        .map { it[GATES_PAUSED_UNTIL_KEY] ?: 0L }

    suspend fun setGatesPausedUntilMillis(millis: Long) {
        context.settingsDataStore.edit { it[GATES_PAUSED_UNTIL_KEY] = millis }
    }

    /**
     * T-13 (tepera-dev-spec.md), "рушій карток": скільки разів поспіль подієва картка (пауза)
     * витіснила тижневу картку-оцінку зі стеку — `CardEngine`/`CardHistoryRepository` звіряють
     * це між викликами `selectVisible()` (не лише в межах одного відкриття Home), щоб правило
     * "не витісняють тижневі більш ніж двічі поспіль" рахувало реальну послідовність днів, а не
     * скидалось щоразу, коли застосунок перезапускається.
     */
    val cardEventDisplacementStreak: Flow<Int> = context.settingsDataStore.data
        .map { it[CARD_EVENT_DISPLACEMENT_STREAK_KEY] ?: 0 }

    suspend fun setCardEventDisplacementStreak(value: Int) {
        context.settingsDataStore.edit { it[CARD_EVENT_DISPLACEMENT_STREAK_KEY] = value }
    }
}
