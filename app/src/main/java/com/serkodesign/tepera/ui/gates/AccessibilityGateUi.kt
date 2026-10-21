package com.serkodesign.tepera.ui.gates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaSymbols

/** Тестова збірка (a11y): помітне пояснення ПЕРЕД відправкою в системні налаштування доступності. */
@Composable
fun AccessibilityDisclosureDialog(onDismiss: () -> Unit, onContinue: () -> Unit) {
    TeperaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.a11y_disclosure_title),
        text = stringResource(R.string.a11y_disclosure_body),
        confirmText = stringResource(R.string.a11y_disclosure_continue),
        onConfirm = onContinue,
        dismissText = stringResource(R.string.a11y_disclosure_cancel)
    )
}

/** Картка на екрані «Застосунки з затримкою»: стан доступу й кнопка. */
@Composable
fun AccessibilityGateCard(enabled: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    TeperaCard(modifier = modifier, title = stringResource(R.string.a11y_gate_card_title)) {
        Text(
            text = stringResource(if (enabled) R.string.a11y_gate_card_on else R.string.a11y_gate_card_off),
            style = MaterialTheme.typography.bodyMedium
        )
        TeperaButton(
            text = stringResource(if (enabled) R.string.a11y_gate_manage_action else R.string.a11y_gate_enable_action),
            onClick = onOpen,
            type = if (enabled) TeperaButtonType.Secondary else TeperaButtonType.Primary
        )
    }
}

/** Картка «Відкриття без затримки»: − / + і лічильник за сьогодні. 0 = вимкнено. */
@Composable
fun FreeOpensCard(perDay: Int, usedToday: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    TeperaCard(modifier = modifier, title = stringResource(R.string.free_opens_title)) {
        Text(text = stringResource(R.string.free_opens_hint), style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeperaIconButton(
                icon = TeperaSymbols.Remove,
                contentDescription = stringResource(R.string.free_opens_decrease),
                onClick = { onChange((perDay - 1).coerceAtLeast(0)) },
                enabled = perDay > 0
            )
            Text(
                text = if (perDay == 0) stringResource(R.string.free_opens_off) else stringResource(R.string.free_opens_count_format, perDay),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TeperaIconButton(
                icon = TeperaSymbols.Add,
                contentDescription = stringResource(R.string.free_opens_increase),
                onClick = { onChange((perDay + 1).coerceAtMost(MAX_FREE_OPENS)) },
                enabled = perDay < MAX_FREE_OPENS
            )
        }
        if (perDay > 0) {
            Text(
                text = stringResource(R.string.free_opens_used_format, usedToday.coerceAtMost(perDay), perDay),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private const val MAX_FREE_OPENS = 10
