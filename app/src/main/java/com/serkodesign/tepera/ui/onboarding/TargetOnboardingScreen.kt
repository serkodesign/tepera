package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.ui.theme.HourRangeSlider
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaOnboardingTitle
import com.serkodesign.tepera.util.TargetSuggestion
import kotlinx.coroutines.launch

/**
 * CC-1: крок онбордингу «Орієнтир на день» — ПІСЛЯ дозволу на доступ до статистики (потрібна історія,
 * щоб показати власне середнє). Дві РІВНОЦІННІ кнопки («Пізніше» і «Задати» — той самий тип, той самий
 * розмір, жодної з них не виділено). Стартове значення повзунка — середнє самої людини за останні дні,
 * округлене до години, без оцінки; без історії середнє не згадується, а повзунок стартує з
 * нейтрального значення. 
 * «Пізніше» лишає орієнтира порожнім — ніде не з'являється число-ціль; змінити можна в Налаштуваннях.
 */
@Composable
fun TargetOnboardingScreen(
    settingsStore: SettingsStore,
    balanceRepository: BalanceRepository,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var average by remember { mutableStateOf<BalanceRepository.OnlineAverage?>(null) }
    var hours by remember { mutableStateOf(TargetSuggestion.NEUTRAL_START_HOURS) }

    LaunchedEffect(Unit) {
        average = balanceRepository.averageDailyOnline()
        average?.let { hours = TargetSuggestion.hoursFor(it.minutesPerDay) }
        loaded = true
    }

    fun finish(setTarget: Boolean) {
        scope.launch {
            if (setTarget) {
                settingsStore.setTargetMinutes(hours * 60)
            } else {
                settingsStore.setTargetMinutes(null)
            }
            settingsStore.setTargetOnboardingSeen()
            onDone()
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TeperaOnboardingTitle(text = stringResource(R.string.target_onboarding_title))
            Text(
                text = stringResource(R.string.target_onboarding_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (loaded) {
                TeperaCard(modifier = Modifier.padding(top = 24.dp)) {
                    average?.let {
                        Text(
                            text = stringResource(
                                R.string.target_onboarding_average_format,
                                it.days,
                                stringResource(R.string.hours_short_format, TargetSuggestion.hoursFor(it.minutesPerDay))
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HourRangeSlider(
                        hours = hours,
                        onHoursChange = { hours = it },
                        valueLabel = { value -> stringResource(R.string.settings_target_hours_format, value) },
                        minHours = TargetSuggestion.MIN_HOURS,
                        maxHours = TargetSuggestion.MAX_HOURS
                    )
                }
            }
            // Рівноцінні варіанти: однаковий тип і розмір.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeperaButton(
                    text = stringResource(R.string.target_onboarding_later),
                    onClick = { finish(setTarget = false) },
                    modifier = Modifier.weight(1f),
                    type = TeperaButtonType.Secondary
                )
                TeperaButton(
                    text = stringResource(R.string.target_onboarding_set),
                    onClick = { finish(setTarget = true) },
                    modifier = Modifier.weight(1f),
                    type = TeperaButtonType.Secondary,
                    enabled = loaded
                )
            }
        }
    }
}
