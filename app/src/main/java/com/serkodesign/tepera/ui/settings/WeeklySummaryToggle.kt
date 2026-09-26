package com.serkodesign.tepera.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.WeeklySummaryWorker
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.teperaSwitchColors
import kotlinx.coroutines.launch

/**
 * CC-8: перемикач «Тижневий підсумок» — раз на тиждень одне тихе сповіщення. Вимкнений за замовчуванням.
 * На Android 13+ дозвіл на сповіщення запитується ТІЛЬКИ тут, після того як людина сама ввімкнула
 * підсумок. Відмова нічого не ламає: перемикач лишається вимкненим, поруч — спокійна підказка.
 */
@Composable
fun WeeklySummaryToggle(settingsStore: SettingsStore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by settingsStore.weeklySummaryEnabled.collectAsState(initial = false)
    var permissionDenied by remember { mutableStateOf(false) }

    fun enable() {
        scope.launch {
            settingsStore.setWeeklySummaryEnabled(true)
            WeeklySummaryWorker.schedule(context)
            permissionDenied = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enable() else permissionDenied = true
    }

    // Дозвіл могли вимкнути в системі вже після ввімкнення підсумку — перемикач лишається чесним.
    LaunchedEffect(enabled) {
        if (enabled && !WeeklySummaryWorker.canNotify(context)) permissionDenied = true
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassRow(
            label = stringResource(R.string.weekly_summary_label),
            leading = { TeperaIconCircle(TeperaSymbols.Notifications) },
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = { wantsOn ->
                        if (!wantsOn) {
                            scope.launch {
                                settingsStore.setWeeklySummaryEnabled(false)
                                WeeklySummaryWorker.cancel(context)
                                permissionDenied = false
                            }
                        } else if (Build.VERSION.SDK_INT >= 33 && !WeeklySummaryWorker.canNotify(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            enable()
                        }
                    },
                    colors = teperaSwitchColors()
                )
            }
        )
        Text(
            text = stringResource(
                if (permissionDenied) R.string.weekly_summary_permission_hint else R.string.weekly_summary_hint
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
