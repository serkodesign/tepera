package com.serkodesign.tepera.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAnalyticsTest {

    private class FakeClient : RemoteClient {
        val captured = mutableListOf<Pair<String, Map<String, Any>>>()
        var calls = 0
        override fun capture(name: String, props: Map<String, Any>) {
            calls++
            captured += name to props
        }
        override fun setOptOut(optOut: Boolean) { calls++ }
        override fun distinctId(): String { calls++; return "id" }
        override fun reset() { calls++ }
    }

    private class FakeConsent(var enabled: Boolean) : AnalyticsConsent {
        override fun isEnabled() = enabled
    }

    private val categoryA = AnalyticsEvent.TabViewed(Tab.DATA)
    private val categoryB = AnalyticsEvent.IntentionSelected(IntentionBucket.MIN_15)

    @Test
    fun without_consent_sdk_is_never_called() {
        val client = FakeClient()
        val remote = RemoteAnalytics(client, FakeConsent(false), "1.0")
        remote.track(categoryA)
        remote.track(categoryB)
        remote.track(AnalyticsEvent.OnboardingStarted)
        assertEquals(0, client.calls)
    }

    @Test
    fun without_client_nothing_happens_and_nothing_throws() {
        val remote = RemoteAnalytics(null, FakeConsent(true), "1.0")
        remote.track(categoryA)
    }

    @Test
    fun category_b_is_not_sent_by_default_even_with_consent() {
        val client = FakeClient()
        val remote = RemoteAnalytics(client, FakeConsent(true), "1.0")
        remote.track(categoryB)
        remote.track(AnalyticsEvent.RiskWindowStarted)
        remote.track(AnalyticsEvent.SoftExitShown)
        remote.track(AnalyticsEvent.PuzzleShown(PuzzleType.MATH))
        assertEquals(0, client.calls)
    }

    @Test
    fun category_b_goes_only_when_explicitly_allowed() {
        val client = FakeClient()
        RemoteAnalytics(client, FakeConsent(true), "1.0", allowCategoryB = true).track(categoryB)
        assertEquals(listOf("intention_selected"), client.captured.map { it.first })
    }

    @Test
    fun category_a_goes_with_consent_and_carries_only_allowlisted_props_plus_app_version() {
        val client = FakeClient()
        RemoteAnalytics(client, FakeConsent(true), "1.4.2").track(categoryA)
        val (name, props) = client.captured.single()
        assertEquals("tab_viewed", name)
        assertEquals(mapOf<String, Any>("tab" to "data", "app_version" to "1.4.2"), props)
        assertTrue(props.keys.all { it in AnalyticsAllowlist.KEYS })
    }

    @Test
    fun consent_withdrawn_stops_sending_immediately() {
        val client = FakeClient()
        val consent = FakeConsent(true)
        val remote = RemoteAnalytics(client, consent, "1.0")
        remote.track(categoryA)
        consent.enabled = false
        remote.track(categoryA)
        assertEquals(1, client.captured.size)
    }

    @Test
    fun allowlist_drops_unknown_keys_and_non_primitive_values() {
        val dirty: Map<String, Any> = mapOf(
            "tab" to "data",
            "package_name" to "com.instagram.android",
            "app_label" to "Instagram",
            "note" to "free text",
            "duration" to 42,                 // не String/Boolean — відкидається
            "enabled" to true
        )
        assertEquals(mapOf<String, Any>("tab" to "data", "enabled" to true), AnalyticsAllowlist.sanitize(dirty))
    }
}
