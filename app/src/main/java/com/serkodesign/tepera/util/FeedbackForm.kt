package com.serkodesign.tepera.util

import android.content.Context

/**
 * FR-6.4–6.6 (SRS v2.7): пункт "Запропонувати функцію" — Google Form через `Intent.ACTION_VIEW`,
 * без `INTERNET` у маніфесті (мережею користується браузер зі своїм дозволом, застосунок лише
 * передає намір системі). Дві окремі форми під UA/EN (FR-6.5), без передзаповнення полів.
 *
 * TODO(FR-6.4): за прямим запитом користувача обидві мови ТИМЧАСОВО ведуть на ОДНУ й ту саму
 * форму (стейкхолдер ще не створив окрему англомовну) — замінити [EN_FORM_URL] на окрему форму,
 * коли вона з'явиться. [UA_FORM_URL] — реальна форма.
 */
object FeedbackForm {
    private const val UA_FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSceU-T5fFIcPIHsPBpgmC7s2aWplM8WZMlXArO5ykStnunl8A/viewform?usp=publish-editor"
    private const val EN_FORM_URL = UA_FORM_URL

    /** Мова форми йде за поточною мовою застосунку (LocaleStore), не за системною, якщо обрано вручну. */
    fun urlFor(context: Context): String {
        val explicitTag = LocaleStore.getLanguageTag(context)
        val languageTag = explicitTag.ifEmpty { java.util.Locale.getDefault().language }
        return if (languageTag.startsWith("uk")) UA_FORM_URL else EN_FORM_URL
    }
}
