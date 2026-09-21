package com.serkodesign.tepera.util

import android.content.Context

/**
 * FR-6.4–6.6 (SRS v2.7): пункт "Запропонувати функцію" — Google Form через `Intent.ACTION_VIEW`,
 * без `INTERNET` у маніфесті (мережею користується браузер зі своїм дозволом, застосунок лише
 * передає намір системі). Дві окремі форми під UA/EN (FR-6.5), без передзаповнення полів.
 *
 * Дві окремі форми: [UA_FORM_URL] ("Чого не вистачає Tepera?") і [EN_FORM_URL] ("What do you wanna see in Tepera?").
 */
object FeedbackForm {
    private const val UA_FORM_URL = "https://forms.gle/gyUGVProSwdgFpcMA"
    private const val EN_FORM_URL = "https://forms.gle/wwdRU1M6ejiFEkZN9"

    /** Мова форми йде за поточною мовою застосунку (LocaleStore), не за системною, якщо обрано вручну. */
    fun urlFor(context: Context): String {
        val explicitTag = LocaleStore.getLanguageTag(context)
        val languageTag = explicitTag.ifEmpty { java.util.Locale.getDefault().language }
        return if (languageTag.startsWith("uk")) UA_FORM_URL else EN_FORM_URL
    }
}
