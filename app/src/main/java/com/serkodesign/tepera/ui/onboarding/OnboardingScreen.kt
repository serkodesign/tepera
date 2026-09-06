package com.serkodesign.tepera.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore

/** FR-7.1: явний онбординг-екран доступу до статистики використання, показується один раз. */
@Composable
fun OnboardingScreen(
    settingsStore: SettingsStore,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    // "Показано" фіксується одразу при відкритті екрана — незалежно від того, чи користувач
    // натисне "Надати доступ", чи "Пропустити", чи просто піде назад системним back.
    LaunchedEffect(Unit) {
        settingsStore.setOnboardingUsageAccessSeen()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Balance,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.padding(top = 16.dp))
            Text(
                text = stringResource(R.string.onboarding_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.padding(top = 12.dp))
            Text(
                text = stringResource(R.string.onboarding_explanation),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.padding(top = 32.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.usage_access_open_settings))
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
    }
}
