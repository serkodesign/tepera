package com.serkodesign.tepera.ui.risk

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.WeeklySummaryWorker
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaHint

/**
 * v3.0 MVP, Етап 4: пояснення перед системним запитом `POST_NOTIFICATIONS`. Без дозволу все працює, крім самих
 * нагадувань. Запит показується лише після тапу на кнопку; відмова нічого не ламає. Планування нагадувань ще не
 * реалізовано — екран готує дозвіл.
 */
@Composable
fun NudgeInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(WeeklySummaryWorker.canNotify(context)) }

    // Стан дозволу міг змінитись у системних налаштуваннях — перевіряємо щоразу, коли екран знову видимий.
    LifecycleResumeEffect(Unit) {
        allowed = WeeklySummaryWorker.canNotify(context)
        onPauseOrDispose {}
    }

    // Дозвіл з’явився (діалог чи системні налаштування) — нагадування плануються одразу.
    androidx.compose.runtime.LaunchedEffect(allowed) {
        if (allowed) com.serkodesign.tepera.data.NudgeScheduler.reschedule(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = WeeklySummaryWorker.canNotify(context)
    }

    fun openSystemSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.v3_nudge_title), onBack = onBack)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeperaCard(
                    title = stringResource(if (allowed) R.string.v3_nudge_status_on else R.string.v3_nudge_status_off)
                ) {
                    Text(stringResource(R.string.v3_nudge_explain), style = MaterialTheme.typography.bodyMedium)
                    if (!allowed) {
                        TeperaButton(
                            text = stringResource(
                                if (Build.VERSION.SDK_INT >= 33) R.string.v3_nudge_allow else R.string.v3_nudge_open_settings
                            ),
                            onClick = {
                                // Після відмови Android 13+ уже не показує системний діалог — тоді лише налаштування.
                                if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else openSystemSettings()
                            }
                        )
                        if (Build.VERSION.SDK_INT >= 33) {
                            TeperaButton(
                                text = stringResource(R.string.v3_nudge_open_settings),
                                onClick = ::openSystemSettings,
                                type = TeperaButtonType.Secondary
                            )
                        }
                    }
                }
                TeperaHint(stringResource(R.string.v3_nudge_rules), Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}
