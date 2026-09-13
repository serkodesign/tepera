package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.EstimateType
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * T-3 (tepera-dev-spec.md): "вибір діапазону" для питання "Скільки, по-твоєму, ти був онлайн
 * учора?" — [representativeMinutes] лишається лише всередині застосунку (порівняння з реальним
 * числом, ContextCardStack), у самому питанні людина обирає ТІЛЬКИ діапазон, ніколи не бачить
 * "перетворену" цифру у власному виборі до розкриття розриву (`OnlineEstimateRevealCard` показує
 * назад саме текст діапазону, не representativeMinutes, — `fromRepresentativeMinutes` нижче).
 */
enum class DailyOnlineGuess(val representativeMinutes: Long) {
    UNDER_1H(30),
    ONE_TO_3H(120),
    OVER_3H(240);

    companion object {
        fun fromRepresentativeMinutes(minutes: Long): DailyOnlineGuess? =
            entries.find { it.representativeMinutes == minutes }
    }
}

/**
 * T-3, крок 2 з "Порядку першого запуску": між питанням про цінності (FR-P.2,
 * [ValuesOnboardingScreen]) і поясненням дозволу (FR-7.1, [OnboardingScreen]). Вибір діапазону
 * зберігається як `UserEstimateEntity` (розділ 2.2 документа — "принцип пасивного сорому":
 * реальне число з'явиться пізніше поруч з оцінкою, ContextCardStack, не пасивно й не одразу).
 * "Пропустити" — одна видима кнопка, не зберігає жодного рядка (нема з чим порівнювати пізніше).
 */
@Composable
fun OnlineEstimateOnboardingScreen(
    settingsStore: SettingsStore,
    userEstimateRepository: UserEstimateRepository,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    fun finish(guess: DailyOnlineGuess?) {
        scope.launch {
            if (guess != null) {
                userEstimateRepository.saveEstimate(
                    type = EstimateType.ONLINE_HOURS,
                    estimatedValue = guess.representativeMinutes,
                    forDate = LocalDate.now().minusDays(1)
                )
            }
            settingsStore.setOnlineEstimateOnboardingSeen()
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
            Text(
                text = stringResource(R.string.online_estimate_onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DailyOnlineGuess.entries.forEach { guess ->
                    OutlinedButton(onClick = { finish(guess) }, modifier = Modifier.weight(1f)) {
                        Text(dailyGuessLabel(guess))
                    }
                }
            }
            TextButton(onClick = { finish(null) }, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
    }
}

@Composable
internal fun dailyGuessLabel(guess: DailyOnlineGuess): String = when (guess) {
    DailyOnlineGuess.UNDER_1H -> stringResource(R.string.online_estimate_range_under_1h)
    DailyOnlineGuess.ONE_TO_3H -> stringResource(R.string.online_estimate_range_1_3h)
    DailyOnlineGuess.OVER_3H -> stringResource(R.string.online_estimate_range_over_3h)
}
