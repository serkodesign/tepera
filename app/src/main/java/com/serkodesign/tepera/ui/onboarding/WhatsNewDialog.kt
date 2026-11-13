package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * 1.2.1: одноразове вікно «Що змінилось» для тих, хто оновився з попередніх версій (список — відносно 1.0.5).
 * Умову показу й прапорець `whatsNew121Seen` веде [com.serkodesign.tepera.ui.navigation.TeperaNavHost].
 */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val items = listOf(
        R.string.whats_new_home_lead to R.string.whats_new_home_body,
        R.string.whats_new_hours_lead to R.string.whats_new_hours_body,
        R.string.whats_new_pause_lead to R.string.whats_new_pause_body,
        R.string.whats_new_detox_lead to R.string.whats_new_detox_body,
        R.string.whats_new_stats_lead to R.string.whats_new_stats_body,
        R.string.whats_new_look_lead to R.string.whats_new_look_body,
        R.string.whats_new_widgets_lead to R.string.whats_new_widgets_body
    )
    TeperaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.whats_new_title),
        confirmText = stringResource(R.string.whats_new_confirm),
        onConfirm = onDismiss
    ) {
        Text(
            stringResource(R.string.whats_new_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (lead, body) ->
                val leadText = stringResource(lead)
                val bodyText = stringResource(body)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(leadText) }
                        append(" — ")
                        append(bodyText)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
                )
            }
        }
    }
}
