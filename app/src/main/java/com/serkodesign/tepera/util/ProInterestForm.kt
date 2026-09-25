package com.serkodesign.tepera.util

import android.content.Context

/**
 * CC-11: Google Form «Хочу дізнатись» для екрана «Tepera Pro — у розробці» (fake door). Відкривається
 * лише через `Intent.ACTION_VIEW`, без `INTERNET`, без передзаповнення полів.
 *
 * **Окремої форми власник ще не надав** ([UA_FORM_URL]/[EN_FORM_URL] = `null`) — тимчасово відкривається
 * та сама форма, що й «Запропонувати функцію» ([FeedbackForm]). Щойно з'являться справжні посилання —
 * вписати їх сюди, більше нічого змінювати не треба.
 */
object ProInterestForm {
    private val UA_FORM_URL: String? = null
    private val EN_FORM_URL: String? = null

    fun urlFor(context: Context): String {
        val languageTag = LocaleStore.getLanguageTag(context).ifEmpty { java.util.Locale.getDefault().language }
        val own = if (languageTag.startsWith("uk")) UA_FORM_URL else EN_FORM_URL
        return own ?: FeedbackForm.urlFor(context)
    }
}
