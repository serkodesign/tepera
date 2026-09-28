package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.theme.OnboardingSkipAllButton
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaOnboardingTitle
import com.serkodesign.tepera.ui.theme.TeperaSymbols

/**
 * Останній крок онбордингу (за прямим запитом користувача) — коротка згадка про ворота
 * ("Застосунки з затримкою"), після пропозиції віджета. Суто інформаційний: одна кнопка
 * "Зрозуміло", без окремого налаштування тут — саму функцію й далі вмикають з
 * Налаштування → Застосунки з затримкою (`GatesScreen`), як і зараз. "Показано" фіксується
 * одразу при відкритті, той самий принцип, що [WidgetSuggestionScreen].
 */
@Composable
fun GatesOnboardingScreen(
    settingsStore: SettingsStore,
    onDone: () -> Unit,
    onSkipAll: () -> Unit
) {
    LaunchedEffect(Unit) {
        settingsStore.setGatesOnboardingSeen()
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TeperaIconCircle(icon = TeperaSymbols.Timer, size = 64.dp)
                TeperaOnboardingTitle(
                    text = stringResource(R.string.gates_onboarding_title),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = stringResource(R.string.gates_onboarding_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                TeperaButton(
                    text = stringResource(R.string.gates_onboarding_action),
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    type = TeperaButtonType.Primary
                )
            }
            OnboardingSkipAllButton(
                onClick = onSkipAll,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
