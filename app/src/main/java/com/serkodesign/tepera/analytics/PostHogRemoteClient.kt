package com.serkodesign.tepera.analytics

import android.content.Context
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

/**
 * PostHog EU Cloud (Франкфурт). Налаштовано мінімально й анонімно:
 *  — старт у стані opt-out (нічого не надсилається, доки [setOptOut] не зніме); стан узгоджує TeperaApp зі згодою;
 *  — без профілів людей, `identify()`/`alias()` ніде не викликаються;
 *  — без автозбору (життєвий цикл, екрани, deep link), без запису сесій, без feature flags;
 *  — GeoIP вимкнено на рівні кожної події (`$geoip_disable`); IP на кожній події підмінено на 0.0.0.0 (`$ip`), тож реальна
 *    адреса не потрапляє у збережену подію навіть без налаштування проєкту (його все одно бажано ввімкнути).
 */
class PostHogRemoteClient(context: Context, apiKey: String, initiallyOptedOut: Boolean, debug: Boolean) : RemoteClient {
    init {
        val config = PostHogAndroidConfig(apiKey = apiKey, host = HOST).apply {
            optOut = initiallyOptedOut
            personProfiles = PersonProfiles.NEVER
            captureApplicationLifecycleEvents = false
            captureScreenViews = false
            captureDeepLinks = false
            sessionReplay = false
            preloadFeatureFlags = false
            // Без запиту remote config на старті (він ішов би до згоди) і без опитувань — нам потрібні лише події.
            remoteConfig = false
            surveys = false
            this.debug = debug
        }
        PostHogAndroid.setup(context.applicationContext, config)
    }

    override fun capture(name: String, props: Map<String, Any>) {
        PostHog.capture(event = name, properties = props + mapOf(GEOIP_DISABLE to true, IP_OVERRIDE to "0.0.0.0"))
    }

    override fun setOptOut(optOut: Boolean) {
        if (optOut) PostHog.optOut() else PostHog.optIn()
    }

    override fun distinctId(): String = PostHog.distinctId()

    override fun reset() {
        PostHog.reset()
    }

    private companion object {
        const val HOST = "https://eu.i.posthog.com"
        const val GEOIP_DISABLE = "\$geoip_disable"
        const val IP_OVERRIDE = "\$ip"
    }
}
