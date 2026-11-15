package com.serkodesign.tepera.ui.risk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.teperaGradientBackground
import kotlinx.coroutines.delay

/**
 * Берсерк-режим: один екран і для моменту, коли година набігла (жарт + лічильник), і для повторного відкриття вже
 * заблокованої стрічки (лише лічильник). Жарт про ситуацію, не про людину: застосунок не оцінює.
 *
 * @param joke текст жарту; `null` — повторне відкриття, тоді лише пояснення й лічильник.
 * @param blockedName назва застосунку або `null`, коли заблоковані всі стрічки.
 */
@Composable
fun BerserkBlockScreen(
    joke: String?,
    blockedName: String?,
    untilMillis: Long,
    canSkip: Boolean,
    onOk: () -> Unit,
    onSkipToday: () -> Unit
) {
    var remaining by remember { mutableLongStateOf((untilMillis - System.currentTimeMillis()).coerceAtLeast(0L)) }
    LaunchedEffect(untilMillis) {
        while (true) {
            remaining = (untilMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remaining <= 0L) {
                onOk()
                return@LaunchedEffect
            }
            delay(1_000)
        }
    }
    Column(
        Modifier.fillMaxSize().teperaGradientBackground().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.berserk_title), color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        Text(
            joke ?: stringResource(R.string.berserk_still_blocked),
            color = TeperaPalette.textPrimary, fontSize = 22.sp, lineHeight = 30.sp, textAlign = TextAlign.Center
        )
        Text(formatRemaining(remaining), color = TeperaPalette.textPrimary, fontSize = 56.sp, lineHeight = 64.sp)
        Text(
            if (blockedName != null) stringResource(R.string.berserk_resting_format, blockedName) else stringResource(R.string.berserk_resting_all),
            color = TeperaPalette.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
        )
        TeperaButton(text = stringResource(R.string.berserk_ok), onClick = onOk, modifier = Modifier.fillMaxWidth())
        if (canSkip) {
            TeperaButton(
                text = stringResource(R.string.berserk_important),
                onClick = onSkipToday,
                type = TeperaButtonType.Tertiary,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.berserk_important_hint),
                color = TeperaPalette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center
            )
        }
    }
}

/** «58:12» або «1:05:00», коли лишилось більше години. */
internal fun formatRemaining(millis: Long): String {
    val total = millis / 1_000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(java.util.Locale.ROOT, "%02d:%02d", m, s)
}
