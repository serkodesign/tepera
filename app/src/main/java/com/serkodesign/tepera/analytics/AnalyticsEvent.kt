package com.serkodesign.tepera.analytics

/**
 * Каталог подій аналітики (платформо-нейтральний: ті самі назви знадобляться на iOS).
 *
 * Події типізовані: неможливо передати довільну назву чи властивість. Значення властивостей — лише з enum-ів
 * нижче (без назв застосунків, тексту користувача, точних тривалостей).
 */
enum class Category {
    /** Власний UI Tepera. */
    A_OWN_UI,

    /** Походить з AccessibilityService / UsageStats. За замовчуванням ТІЛЬКИ локально. */
    B_SYSTEM_DERIVED
}

enum class OnboardingStep { WELCOME, ACCESSIBILITY, CATEGORIES, ONLINE_ESTIMATE, TARGET, FEEDS, PERMISSIONS, GATES }
enum class Tab { TODAY, ACTIVITIES, DATA, SETTINGS }
enum class PauseDuration { TODAY, WEEKEND, UNTIL_DATE }
enum class IntentionBucket { MIN_5, MIN_15, MIN_30, OTHER }
enum class SoftExitOutcome { LEFT, CONTINUED }
enum class PuzzleType { SUDOKU, MATH, SCHULTE, DIGITS, SEQUENCE, INTENTION, OTHER }
enum class WidgetKind {
    BUTTONS_1X1, BUTTONS_2X1, BUTTONS_4X1, ONLINE_RING, ORB, NEXT_HOUR, IDEA,
    ACTIVE_NOW, SKIP_TODAY, GATE_PAUSE, ADD_ENTRY, WEEK, PAUSES
}

/** Значення властивості: рядок у snake_case або bool. Інших типів назовні не йде. */
sealed class AnalyticsEvent(
    val name: String,
    val category: Category,
    val props: Map<String, Any> = emptyMap()
) {
    // ---- Категорія A ----
    object OnboardingStarted : AnalyticsEvent("onboarding_started", Category.A_OWN_UI)
    class OnboardingStepCompleted(step: OnboardingStep) :
        AnalyticsEvent("onboarding_step_completed", Category.A_OWN_UI, mapOf("step" to step.key()))
    object OnboardingCompleted : AnalyticsEvent("onboarding_completed", Category.A_OWN_UI)
    object AccessibilityServiceEnabled : AnalyticsEvent("accessibility_service_enabled", Category.A_OWN_UI)
    object UsageAccessGranted : AnalyticsEvent("usage_access_granted", Category.A_OWN_UI)
    /** Вікно скролінгу додано (замість «плану якщо-то», якого в застосунку нема). */
    class WindowAdded(isFirst: Boolean) :
        AnalyticsEvent("window_added", Category.A_OWN_UI, mapOf("is_first" to isFirst))
    object WindowEdited : AnalyticsEvent("window_edited", Category.A_OWN_UI)
    object WindowDeleted : AnalyticsEvent("window_deleted", Category.A_OWN_UI)
    object ActivityAdded : AnalyticsEvent("activity_added", Category.A_OWN_UI)
    class TabViewed(tab: Tab) : AnalyticsEvent("tab_viewed", Category.A_OWN_UI, mapOf("tab" to tab.key()))
    class GatePaused(duration: PauseDuration) :
        AnalyticsEvent("gate_paused", Category.A_OWN_UI, mapOf("duration" to duration.key()))
    class WidgetAdded(widget: WidgetKind) :
        AnalyticsEvent("widget_added", Category.A_OWN_UI, mapOf("widget" to widget.key()))
    object QsTileAdded : AnalyticsEvent("qs_tile_added", Category.A_OWN_UI)
    object FeedbackFormOpened : AnalyticsEvent("feedback_form_opened", Category.A_OWN_UI)
    object StatsShareOpened : AnalyticsEvent("stats_share_opened", Category.A_OWN_UI)
    object StatsShared : AnalyticsEvent("stats_shared", Category.A_OWN_UI)
    class AnalyticsConsentChanged(enabled: Boolean) :
        AnalyticsEvent("analytics_consent_changed", Category.A_OWN_UI, mapOf("enabled" to enabled))

    // ---- Категорія B ----
    object RiskWindowStarted : AnalyticsEvent("risk_window_started", Category.B_SYSTEM_DERIVED)
    object IntentionSheetShown : AnalyticsEvent("intention_sheet_shown", Category.B_SYSTEM_DERIVED)
    class IntentionSelected(bucket: IntentionBucket) :
        AnalyticsEvent("intention_selected", Category.B_SYSTEM_DERIVED, mapOf("bucket" to bucket.key()))
    object SoftExitShown : AnalyticsEvent("soft_exit_shown", Category.B_SYSTEM_DERIVED)
    class SoftExitOutcomeEvent(outcome: SoftExitOutcome) :
        AnalyticsEvent("soft_exit_outcome", Category.B_SYSTEM_DERIVED, mapOf("outcome" to outcome.key()))
    class PuzzleShown(type: PuzzleType) :
        AnalyticsEvent("puzzle_shown", Category.B_SYSTEM_DERIVED, mapOf("type" to type.key()))
    class PuzzleSolved(type: PuzzleType) :
        AnalyticsEvent("puzzle_solved", Category.B_SYSTEM_DERIVED, mapOf("type" to type.key()))
    class PuzzleAbandoned(type: PuzzleType) :
        AnalyticsEvent("puzzle_abandoned", Category.B_SYSTEM_DERIVED, mapOf("type" to type.key()))
}

private fun Enum<*>.key(): String = name.lowercase()
