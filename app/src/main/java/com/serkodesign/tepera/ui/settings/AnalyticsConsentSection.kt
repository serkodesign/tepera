package com.serkodesign.tepera.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaHint
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.ui.theme.teperaSwitchColors

/**
 * «Допомогти покращити Tepera» (рівень 2 аналітики, PostHog). Вимкнено за замовчуванням (opt-in).
 * Показується лише в збірці, де задано ключ проєкту; без ключа віддаленої аналітики нема взагалі — блок прихований.
 * Видалення — одна кнопка «Видалити мої дані аналітики» (вимкнути + скинути ID + готовий лист із ID).
 */
@Composable
fun AnalyticsConsentSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    if (!app.remoteAnalyticsAvailable) return
    var enabled by remember { mutableStateOf(app.analyticsConsent.isEnabled()) }
        Column(modifier = modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassRow(
            label = stringResource(R.string.analytics_consent_label),
            leading = { TeperaIconCircle(TeperaSymbols.Info) },
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        app.setAnalyticsConsent(it)
                    },
                    colors = teperaSwitchColors()
                )
            }
        )
        TeperaHint(
            text = stringResource(R.string.analytics_consent_explanation),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (enabled || app.analyticsConsent.wasEverEnabled()) {
            TeperaButton(
                text = stringResource(R.string.analytics_delete_action),
                onClick = {
                    // Один тап: вимкнути, скинути ID (старі події відв'язуються від людини) і відкрити готовий лист
                    // із ID, щоб ми стерли їх у PostHog. Листа можна не надсилати — вже скинуто.
                    val oldId = app.analyticsId()
                    enabled = false
                    app.setAnalyticsConsent(false)
                    app.resetAnalyticsId()
                    sendDeletionRequest(context, oldId)
                },
                type = TeperaButtonType.Secondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            TeperaHint(
                text = stringResource(R.string.analytics_delete_hint),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/** Готовий лист із ID у тексті; без поштового застосунку — ID копіюється в буфер, і людина бачить, що робити далі. */
private fun sendDeletionRequest(context: Context, oldId: String?) {
    val subject = context.getString(R.string.analytics_delete_mail_subject)
    val body = context.getString(R.string.analytics_delete_mail_body, oldId ?: "—")
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DELETION_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
        Toast.makeText(context, R.string.analytics_delete_done, Toast.LENGTH_LONG).show()
    } catch (e: ActivityNotFoundException) {
        if (oldId != null) copyToClipboard(context, oldId)
        Toast.makeText(context, R.string.analytics_delete_no_mail, Toast.LENGTH_LONG).show()
    }
}

private const val DELETION_EMAIL = "serkodesign@gmail.com"

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("analytics_id", text))
}
