package com.serkodesign.tepera.domain.risk

/**
 * v3.0 MVP, Етап 1/3: підказки списку застосунків-стрічок. Підказка — лише порядок у списку; увімкнути застосунок
 * може тільки людина. Месенджери не пропонуються (вони мають бути вимкнені за замовчуванням), але лишаються у
 * загальному списку, якщо людина сама їх обере.
 */
object FeedSuggestions {
    /** Відомі месенджери й дзвінки (прийнятий список, не вичерпний). */
    val MESSENGERS: Set<String> = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.telegram.messenger.web",
        "org.thunderdog.challegram", "com.viber.voip", "org.thoughtcrime.securesms", "com.facebook.orca",
        "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.discord", "com.skype.raider",
        "us.zoom.videomeetings", "com.google.android.apps.tachyon", "com.microsoft.teams", "com.slack"
    )

    /** Вечірні слоти: 18:00–04:00 (слоти 72..95 та 0..15). */
    fun isEveningSlot(slot: Int): Boolean = slot >= 72 || slot < 16

    /**
     * До [limit] пакетів з найбільшим вечірнім часом, без месенджерів і застосунків з нульовим вечірнім часом.
     * [eveningSeconds] — секунди у вечірніх слотах за пакетом.
     */
    fun top(eveningSeconds: Map<String, Long>, limit: Int = 12): List<String> =
        eveningSeconds.entries
            .filter { it.value > 0 && it.key !in MESSENGERS }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
}
