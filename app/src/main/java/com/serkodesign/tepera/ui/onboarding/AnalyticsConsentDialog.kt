package com.serkodesign.tepera.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaDialog

/**
 * Одноразовий вибір «Допомогти покращити Tepera» (аналітика рівня 2). Без тиску: однакова вага обох кнопок,
 * закриття повз кнопки = «Ні, дякую» (за замовчуванням вимкнено), текст прямо каже, що збирається і що ні.
 * Не чіпає розкриття Accessibility й не є частиною онбордингу дозволів.
 */
@Composable
fun AnalyticsConsentDialog(onChoice: (Boolean) -> Unit) {
    TeperaDialog(
        onDismissRequest = { onChoice(false) },
        title = stringResource(R.string.analytics_consent_dialog_title),
        text = stringResource(R.string.analytics_consent_dialog_body),
        confirmText = stringResource(R.string.analytics_consent_yes),
        onConfirm = { onChoice(true) },
        dismissText = stringResource(R.string.analytics_consent_no),
        onDismiss = { onChoice(false) },
        dismissType = TeperaButtonType.Primary
    )
}
