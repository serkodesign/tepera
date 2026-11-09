package com.serkodesign.tepera.ui.gates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.M3Background
import com.serkodesign.tepera.ui.theme.M3Button
import com.serkodesign.tepera.ui.theme.M3ButtonSize
import com.serkodesign.tepera.ui.theme.M3ButtonType
import com.serkodesign.tepera.ui.theme.TeperaSpacing

/**
 * Повноекранне розкриття використання AccessibilityService (вимога Google Play: prominent disclosure до запиту дозволу).
 * Лише відображення: що робити після вибору, вирішує виклик ([onAgree] — записати згоду й відкрити системні налаштування;
 * [onDismiss] — закрити без запису згоди). Системне «Назад» і жест назад дорівнюють «Не зараз».
 */
@Composable
fun AccessibilityDisclosureScreen(onAgree: () -> Unit, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    M3Background(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = TeperaSpacing.screenHorizontal)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.a11y_disclosure_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                DisclosureSection(R.string.a11y_disclosure_why_label, R.string.a11y_disclosure_why)
                DisclosureSection(R.string.a11y_disclosure_sees_label, R.string.a11y_disclosure_sees)
                DisclosureSection(R.string.a11y_disclosure_not_label, R.string.a11y_disclosure_not)
                DisclosureSection(R.string.a11y_disclosure_data_label, R.string.a11y_disclosure_data)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.a11y_disclosure_footer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.a11y_disclosure_where),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            M3Button(
                type = M3ButtonType.Filled,
                size = M3ButtonSize.M,
                text = stringResource(R.string.a11y_disclosure_continue),
                onClick = onAgree,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            M3Button(
                type = M3ButtonType.Text,
                size = M3ButtonSize.M,
                text = stringResource(R.string.a11y_disclosure_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DisclosureSection(labelRes: Int, bodyRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp)
    )
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}
