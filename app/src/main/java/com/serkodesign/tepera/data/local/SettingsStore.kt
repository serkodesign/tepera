package com.serkodesign.tepera.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.serkodesign.tepera.data.GapSensitivity
import com.serkodesign.tepera.util.GateSchedule
import com.serkodesign.tepera.util.PauseWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val TARGET_MINUTES_KEY = intPreferencesKey("target_minutes")
private val ONBOARDING_USAGE_ACCESS_SEEN_KEY = booleanPreferencesKey("onboarding_usage_access_seen")
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
private val GATES_PAUSED_FROM_KEY = longPreferencesKey("gates_paused_from")
private val GATE_PAUSE_HISTORY_KEY = stringPreferencesKey("gate_pause_history")
private val GATE_SCHEDULE_KEY = stringPreferencesKey("gate_schedule")
private const val GATE_PAUSE_HISTORY_LIMIT = 40
private val LAST_OPEN_KEY = longPreferencesKey("last_open_millis")
private val GATE_TEXT_BAG_KEY = stringPreferencesKey("gate_text_bag")
private val GATE_GROWING_DELAY_KEY = booleanPreferencesKey("gate_growing_delay")
private val WEEKLY_SUMMARY_ENABLED_KEY = booleanPreferencesKey("weekly_summary_enabled")
private val WELCOME_BACK_PENDING_FROM_KEY = longPreferencesKey("welcome_back_pending_from")
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
     * T-8 (tepera-dev-spec.md): чи вже показаний одноразовий вибір категорій на онбордингу
     * (`CategoryOnboardingScreen`) — першим кроком, перед онбординг-оцінкою
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
     * Завершені паузи (фактичний початок і кінець) — потрібні, щоб скан обходів воріт (CC-9) міг
     * дізнатись, чи були ворота активні в минулий момент. Останні [GATE_PAUSE_HISTORY_LIMIT].
     */
    val gatePauseHistory: Flow<List<PauseWindow>> = context.settingsDataStore.data.map { prefs ->
        prefs[GATE_PAUSE_HISTORY_KEY].orEmpty().split(";").mapNotNull { part ->
            val pieces = part.split("-")
            val from = pieces.getOrNull(0)?.toLongOrNull()
            val until = pieces.getOrNull(1)?.toLongOrNull()
            if (from != null && until != null) PauseWindow(from, until) else null
        }
    }

    suspend fun appendGatePauseHistory(window: PauseWindow) {
        context.settingsDataStore.edit { prefs ->
            val existing = prefs[GATE_PAUSE_HISTORY_KEY].orEmpty().split(";").filter { it.isNotBlank() }
            prefs[GATE_PAUSE_HISTORY_KEY] =
                (existing + "${window.fromMillis}-${window.untilMillis}").takeLast(GATE_PAUSE_HISTORY_LIMIT).joinToString(";")
        }
    }

    /** CC-5: розклад воріт (`null` = "завжди"). Формат — [GateSchedule.encode]. */
    val gateSchedule: Flow<GateSchedule?> = context.settingsDataStore.data
        .map { GateSchedule.decode(it[GATE_SCHEDULE_KEY]) }

    suspend fun setGateSchedule(schedule: GateSchedule?) {
        context.settingsDataStore.edit {
            if (schedule == null) it.remove(GATE_SCHEDULE_KEY) else it[GATE_SCHEDULE_KEY] = schedule.encode()
        }
    }

    /** CC-6: «мішок» текстів екрана паузи — індекси, ще не показані в цьому колі (див. [com.serkodesign.tepera.util.GateTexts]). */
    val gateTextBag: Flow<List<Int>> = context.settingsDataStore.data.map { GateTexts.decode(it[GATE_TEXT_BAG_KEY]) }

    suspend fun setGateTextBag(remaining: List<Int>) {
        context.settingsDataStore.edit { it[GATE_TEXT_BAG_KEY] = GateTexts.encode(remaining) }
    }

    /** CC-8: тижневе сповіщення про підсумок; вимкнене за замовчуванням, вмикається лише самою людиною. */
    val weeklySummaryEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[WEEKLY_SUMMARY_ENABLED_KEY] ?: false }

    suspend fun setWeeklySummaryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[WEEKLY_SUMMARY_ENABLED_KEY] = enabled }
    }

    /** CC-6: «зростаюча» затримка воріт; за замовчуванням вимкнена. */
    val gateGrowingDelay: Flow<Boolean> = context.settingsDataStore.data.map { it[GATE_GROWING_DELAY_KEY] ?: false }

    suspend fun setGateGrowingDelay(enabled: Boolean) {
        context.settingsDataStore.edit { it[GATE_GROWING_DELAY_KEY] = enabled }
    }

    /**
    /**
     * CC-5: пауза воріт — вікно [from, until). Раніше (T-4) був лише кінець "на сьогодні"
     * (`gates_paused_until`), тепер до нього додано початок (`gates_paused_from`), бо "на вихідні"
     * серед тижня починається в суботу. Відсутній `from` (старі значення) = 0, тобто пауза вже діє.
     * `until` = 0 — не на паузі.
     */
    val gatePause: Flow<PauseWindow?> = context.settingsDataStore.data.map { prefs ->
        val until = prefs[GATES_PAUSED_UNTIL_KEY] ?: 0L
        if (until > 0L) PauseWindow(prefs[GATES_PAUSED_FROM_KEY] ?: 0L, until) else null
    }

    suspend fun setGatePause(window: PauseWindow?) {
        context.settingsDataStore.edit {
            if (window == null) {
                it.remove(GATES_PAUSED_UNTIL_KEY)
                it.remove(GATES_PAUSED_FROM_KEY)
            } else {
                it[GATES_PAUSED_FROM_KEY] = window.fromMillis
                it[GATES_PAUSED_UNTIL_KEY] = window.untilMillis
            }
        }
    }
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
