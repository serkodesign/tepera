package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.ui.gates.BreathingBadge
import com.serkodesign.tepera.ui.gates.rememberBreathState
import com.serkodesign.tepera.ui.theme.OnboardingTopBar
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaOnboardingTitle
import com.serkodesign.tepera.ui.theme.extraAnimationsEnabled
import kotlinx.coroutines.delay

private const val DEMO_SECONDS = 5

/**
 * Крок онбордингу про ворота («Застосунки з затримкою», Figma Onboarding / Gates intro). За прямим запитом
 * користувача замість арту з макета — те саме, що бачить людина на справжньому екрані паузи: дихаюча «квітка»
 * (`BreathingBadge`) з числом, що відлічує секунди й зникає, коли очікування минає, після чого цикл повторюється.
 * Суто інформаційний: одна кнопка «Зрозуміло»; саму функцію вмикають у Налаштування → Застосунки з затримкою.
 * "Показано" фіксується одразу при відкритті.
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

    // Демо відліку: 5 → 1, коротка пауза без числа (як на екрані паузи, коли можна продовжувати), і знову.
    // Із вимкненою анімацією (системно або тумблером) число лишається статичним, без циклу.
    val animated = extraAnimationsEnabled()
    var demoNumber by remember { mutableStateOf<Int?>(DEMO_SECONDS) }
    LaunchedEffect(animated) {
        if (!animated) {
            demoNumber = DEMO_SECONDS
            return@LaunchedEffect
        }
        while (true) {
            for (n in DEMO_SECONDS downTo 1) {
                demoNumber = n
                delay(1000)
            }
            demoNumber = null
            delay(1800)
        }
    }
    val breath = rememberBreathState(paused = !animated)
    val demoDescription = stringResource(R.string.gates_onboarding_demo_description)

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BreathingBadge(
                        scale = breath.scale,
                        outerScale = breath.outerScale,
                        number = demoNumber,
                        badgeSize = 190.dp,
                        modifier = Modifier.semantics { contentDescription = demoDescription }
                    )
                    TeperaOnboardingTitle(
                        text = stringResource(R.string.gates_onboarding_title),
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    Text(
                        text = stringResource(R.string.gates_onboarding_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                TeperaButton(
                    text = stringResource(R.string.gates_onboarding_action),
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    type = TeperaButtonType.Primary
                )
            }
            OnboardingTopBar(
                step = 5,
                onSkipAll = onSkipAll,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
