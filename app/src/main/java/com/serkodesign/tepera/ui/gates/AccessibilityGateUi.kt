package com.serkodesign.tepera.ui.gates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.serkodesign.tepera.util.A11yConsentStore
import com.serkodesign.tepera.util.A11yFeatureState
import com.serkodesign.tepera.util.GateAccessibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaSymbols

/**
 * Єдина точка входу до функцій, що потребують служби доступності (розкриття → згода → системні налаштування).
 * Повертає функцію `request`; екран розкриття показується тут же повноекранним вікном.
 * - [onActive] — згода є й службу ввімкнено (або flavor без служби): функцію можна вмикати одразу;
 * - [onAgreed] — людина натиснула «Погоджуюсь» (згоду записано, відкриваються системні налаштування).
 * «Не зараз» / «Назад» нічого не записують і не викликають.
 */
@Composable
fun rememberAccessibilityRequest(onActive: () -> Unit = {}, onAgreed: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    var showDisclosure by remember { mutableStateOf(false) }
    val currentOnActive by rememberUpdatedState(onActive)
    val currentOnAgreed by rememberUpdatedState(onAgreed)
    if (showDisclosure) {
        Dialog(
            onDismissRequest = { showDisclosure = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            AccessibilityDisclosureScreen(
                onAgree = {
                    showDisclosure = false
                    A11yConsentStore.accept(context)
                    GateAccessibility.openSettings(context)
                    currentOnAgreed()
                },
                onDismiss = { showDisclosure = false }
            )
        }
    }
    return {
        when (A11yFeatureState.current(context)) {
            A11yFeatureState.Unavailable, A11yFeatureState.Active -> currentOnActive()
            // Згода вже є, але службу не ввімкнено — показуємо розкриття ще раз (його має бути видно з будь-якої точки).
            A11yFeatureState.NeedsDisclosure, A11yFeatureState.NeedsServiceEnabled -> showDisclosure = true
        }
    }
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
