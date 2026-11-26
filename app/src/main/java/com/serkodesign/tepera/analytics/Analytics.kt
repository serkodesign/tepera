package com.serkodesign.tepera.analytics

interface Analytics {
    fun track(event: AnalyticsEvent)
}

/** Тести та вимкнений стан. */
object NoOpAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit
}

/** Дозволені назовні й у сховище властивості; будь-що інше відкидається в коді. */
object AnalyticsAllowlist {
    val KEYS: Set<String> = setOf(
        "step", "is_first", "tab", "duration", "widget", "enabled", "bucket", "outcome", "type", "app_version"
    )

    /** Лишає лише дозволені ключі зі значеннями String/Boolean. */
    fun sanitize(props: Map<String, Any>): Map<String, Any> =
        props.filter { (k, v) -> k in KEYS && (v is String || v is Boolean) }
}

/** Розсилає подію кільком отримувачам (Local, згодом Remote). Збій одного не ламає інших. */
class CompositeAnalytics(private val sinks: List<Analytics>) : Analytics {
    override fun track(event: AnalyticsEvent) {
        sinks.forEach { runCatching { it.track(event) } }
    }
}
