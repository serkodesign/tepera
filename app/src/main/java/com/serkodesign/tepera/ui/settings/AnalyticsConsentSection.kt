package com.serkodesign.tepera.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
 * Коли ввімкнено: показуємо анонімний ID аналітики (для запиту на видалення) з копіюванням і кнопку «Скинути ID».
 */
@Composable
fun AnalyticsConsentSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TeperaApp
    if (!app.remoteAnalyticsAvailable) return
    var enabled by remember { mutableStateOf(app.analyticsConsent.isEnabled()) }
    var analyticsId by remember { mutableStateOf(app.analyticsId()) }
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
                        analyticsId = app.analyticsId()
                    },
                    colors = teperaSwitchColors()
                )
            }
        )
        TeperaHint(
            text = stringResource(R.string.analytics_consent_explanation),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (enabled && analyticsId != null) {
            GlassRow(
                label = stringResource(R.string.analytics_id_label),
                supporting = analyticsId,
                leading = { TeperaIconCircle(TeperaSymbols.Info) },
                trailing = {},
                onClick = {
                    copyToClipboard(context, analyticsId.orEmpty())
                    Toast.makeText(context, R.string.analytics_id_copied, Toast.LENGTH_SHORT).show()
                }
            )
            TeperaButton(
                text = stringResource(R.string.analytics_id_reset),
                onClick = {
                    app.resetAnalyticsId()
                    analyticsId = app.analyticsId()
                },
                type = TeperaButtonType.Secondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            TeperaHint(
                text = stringResource(R.string.analytics_id_hint),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("analytics_id", text))
}
