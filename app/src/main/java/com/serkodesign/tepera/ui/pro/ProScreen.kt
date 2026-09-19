package com.serkodesign.tepera.ui.pro

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.billing.ProRepository
import com.serkodesign.tepera.data.billing.ProState
import com.serkodesign.tepera.data.billing.RestoreOutcome
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaPalette
import kotlinx.coroutines.launch
import java.util.Date

/**
 * "Tepera Pro" — стан entitlement `tepera_pro` (з інформації про клієнта RevenueCat), вхід у Paywall,
 * Customer Center ("Керувати підпискою", лише коли Pro активний — там скасування/зміна плану/
 * повернення коштів) і "Відновити покупки". За рішенням користувача Pro поки нічого не гейтить.
 * Свідомо тихий екран (Monastic Style): без таймерів, знижок і тиску.
 */
@Composable
fun ProScreen(
    proRepository: ProRepository,
    onOpenPaywall: () -> Unit,
    onOpenCustomerCenter: () -> Unit,
    onBack: () -> Unit
) {
    val state by proRepository.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var restoreMessage by remember { mutableStateOf<Int?>(null) }

    // Стан Pro може змінитись поза застосунком (Play, інший пристрій) — перечитуємо при поверненні.
    LifecycleResumeEffect(Unit) {
        proRepository.refresh()
        onPauseOrDispose { }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.pro_screen_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pro_intro),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeperaPalette.buttonBrandDark
                )

                if (!state.configured) {
                    InfoCard(stringResource(R.string.pro_unavailable))
                } else {
                    InfoCard(statusText(state, context))
                    if (state.loadFailed) InfoCard(stringResource(R.string.pro_status_load_failed))

                    TeperaButton(
                        text = stringResource(R.string.pro_view_action),
                        onClick = onOpenPaywall,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    if (state.isPro) {
                        TeperaButton(
                            text = stringResource(R.string.pro_manage_action),
                            onClick = onOpenCustomerCenter,
                            type = TeperaButtonType.Secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TeperaButton(
                        text = stringResource(R.string.pro_restore_action),
                        onClick = {
                            scope.launch {
                                restoreMessage = when (proRepository.restore()) {
                                    RestoreOutcome.ProRestored -> R.string.pro_restore_done
                                    RestoreOutcome.NothingToRestore -> R.string.pro_restore_nothing
                                    RestoreOutcome.Failed -> R.string.pro_restore_failed
                                }
                            }
                        },
                        type = TeperaButtonType.Tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    restoreMessage?.let { InfoCard(stringResource(it)) }
                }
            }
        }
    }
}

@Composable
private fun statusText(state: ProState, context: android.content.Context): String = when {
    !state.loaded -> stringResource(R.string.pro_status_loading)
    !state.isPro -> stringResource(R.string.pro_status_inactive)
    state.isLifetime -> stringResource(R.string.pro_status_active_lifetime)
    else -> {
        val date = DateFormat.getMediumDateFormat(context).format(Date(state.expiresAtMillis ?: 0L))
        stringResource(
            if (state.willRenew) R.string.pro_status_active_renews else R.string.pro_status_active_ends,
            date
        )
    }
}

/** Спокійна інформаційна картка (білий 80%, як картки Home). */
@Composable
private fun InfoCard(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = TeperaPalette.buttonBrandDark
    )
}
