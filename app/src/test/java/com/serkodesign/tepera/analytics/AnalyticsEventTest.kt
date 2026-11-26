package com.serkodesign.tepera.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventTest {

    private val all: List<AnalyticsEvent> = listOf(
        AnalyticsEvent.OnboardingStarted,
        AnalyticsEvent.OnboardingStepCompleted(OnboardingStep.PERMISSIONS),
        AnalyticsEvent.OnboardingCompleted,
        AnalyticsEvent.AccessibilityServiceEnabled,
        AnalyticsEvent.UsageAccessGranted,
        AnalyticsEvent.WindowAdded(true),
        AnalyticsEvent.WindowEdited,
        AnalyticsEvent.WindowDeleted,
        AnalyticsEvent.ActivityAdded,
        AnalyticsEvent.TabViewed(Tab.DATA),
        AnalyticsEvent.GatePaused(PauseDuration.TODAY),
        AnalyticsEvent.WidgetAdded(WidgetKind.BUTTONS_4X1),
        AnalyticsEvent.QsTileAdded,
        AnalyticsEvent.FeedbackFormOpened,
        AnalyticsEvent.StatsShareOpened,
        AnalyticsEvent.StatsShared,
        AnalyticsEvent.AnalyticsConsentChanged(false),
        AnalyticsEvent.RiskWindowStarted,
        AnalyticsEvent.IntentionSheetShown,
        AnalyticsEvent.IntentionSelected(IntentionBucket.MIN_15),
        AnalyticsEvent.SoftExitShown,
        AnalyticsEvent.SoftExitOutcomeEvent(SoftExitOutcome.LEFT),
        AnalyticsEvent.PuzzleShown(PuzzleType.MATH),
        AnalyticsEvent.PuzzleSolved(PuzzleType.MATH),
        AnalyticsEvent.PuzzleAbandoned(PuzzleType.MATH)
    )

    @Test
    fun names_are_unique_snake_case() {
        val names = all.map { it.name }
        assertEquals(names.size, names.toSet().size)
        names.forEach { assertTrue(it, it.matches(Regex("[a-z0-9]+(_[a-z0-9]+)*"))) }
    }

    @Test
    fun every_event_prop_is_on_the_allowlist() {
        all.forEach { e ->
            assertEquals(e.name, e.props, AnalyticsAllowlist.sanitize(e.props))
        }
    }

    @Test
    fun category_matches_catalog() {
        val b = all.filter { it.category == Category.B_SYSTEM_DERIVED }.map { it.name }.toSet()
        assertEquals(
            setOf(
                "risk_window_started", "intention_sheet_shown", "intention_selected", "soft_exit_shown",
                "soft_exit_outcome", "puzzle_shown", "puzzle_solved", "puzzle_abandoned"
            ),
            b
        )
        assertEquals(17, all.count { it.category == Category.A_OWN_UI })
    }

    @Test
    fun sanitize_drops_unknown_keys_and_non_primitive_values() {
        val out = AnalyticsAllowlist.sanitize(
            mapOf(
                "tab" to "data",
                "package_name" to "com.instagram.android",
                "note" to "free text",
                "duration" to 1234L,
                "enabled" to true
            )
        )
        assertEquals(mapOf("tab" to "data", "enabled" to true), out)
    }

    @Test
    fun composite_survives_failing_sink() {
        var received = 0
        val composite = CompositeAnalytics(
            listOf(
                object : Analytics { override fun track(event: AnalyticsEvent) = error("boom") },
                object : Analytics { override fun track(event: AnalyticsEvent) { received++ } }
            )
        )
        composite.track(AnalyticsEvent.ActivityAdded)
        assertEquals(1, received)
    }
}
