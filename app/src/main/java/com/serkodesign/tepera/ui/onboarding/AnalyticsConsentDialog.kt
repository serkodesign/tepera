package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.analytics.ConsentAnswer
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * Вибір «Допомогти покращити Tepera» (аналітика рівня 2). Без тиску: «Так» і «Ні, дякую» однакової ваги,
 * «Не зараз» — тихий третій варіант (питання повернеться раз, не раніше ніж через 30 днів). Закриття повз кнопки =
 * «Не зараз» (нічого не вмикається). Три короткі пункти про межі збору + розкриття повного списку.
 */
@Composable
fun AnalyticsConsentDialog(onAnswer: (ConsentAnswer) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TeperaDialog(
        onDismissRequest = { onAnswer(ConsentAnswer.LATER) },
        title = stringResource(R.string.analytics_consent_dialog_title),
        text = stringResource(R.string.analytics_consent_dialog_body),
        confirmText = stringResource(R.string.analytics_consent_yes),
        onConfirm = { onAnswer(ConsentAnswer.YES) },
        dismissText = stringResource(R.string.analytics_consent_no),
        onDismiss = { onAnswer(ConsentAnswer.NO) },
        dismissType = TeperaButtonType.Primary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                R.string.analytics_consent_point_1,
                R.string.analytics_consent_point_2,
                R.string.analytics_consent_point_3
            ).forEach { id ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = TeperaPalette.buttonBrandDark)
                    Text(
                        text = stringResource(id),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeperaPalette.buttonBrandDark
                    )
                }
            }
        }
        if (expanded) {
            Text(
                text = stringResource(R.string.analytics_consent_details),
                style = MaterialTheme.typography.bodyMedium,
                color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TeperaButton(
                text = stringResource(
                    if (expanded) R.string.analytics_consent_hide_details else R.string.analytics_consent_show_details
                ),
                onClick = { expanded = !expanded },
                size = TeperaButtonSize.Compact,
                type = TeperaButtonType.Tertiary
            )
            TeperaButton(
                text = stringResource(R.string.analytics_consent_later),
                onClick = { onAnswer(ConsentAnswer.LATER) },
                size = TeperaButtonSize.Compact,
                type = TeperaButtonType.Tertiary
            )
        }
    }
}
