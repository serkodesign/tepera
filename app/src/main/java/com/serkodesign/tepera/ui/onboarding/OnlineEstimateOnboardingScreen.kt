package com.serkodesign.tepera.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.OnboardingTopBar
import com.serkodesign.tepera.ui.theme.TeperaOnboardingTitle
import com.serkodesign.tepera.ui.theme.TeperaButton
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
    UNDER_3H(90),
    THREE_TO_5H(240),
    OVER_5H(360);

    companion object {
        fun fromRepresentativeMinutes(minutes: Long): DailyOnlineGuess? =
            entries.find { it.representativeMinutes == minutes }
    }
}

/**
 * T-3, крок 2 з "Порядку першого запуску": між вибором категорій ([CategoryOnboardingScreen])
 * і поясненням дозволу (FR-7.1, [OnboardingScreen]). Вибір діапазону
 * зберігається як `UserEstimateEntity` (розділ 2.2 документа — "принцип пасивного сорому":
 * реальне число з'явиться пізніше поруч з оцінкою, ContextCardStack, не пасивно й не одразу).
 * "Пропустити" — одна видима кнопка, не зберігає жодного рядка (нема з чим порівнювати пізніше).
 */
@Composable
fun OnlineEstimateOnboardingScreen(
    settingsStore: SettingsStore,
    userEstimateRepository: UserEstimateRepository,
    onDone: () -> Unit,
    onSkipAll: () -> Unit
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

    // Той самий каркас, що кроку категорій: заголовок і підказка зверху, рядки-варіанти на всю ширину
    // (скляні, радіус 24, плитка зліва), тиха дія «Пропустити» знизу.
    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок, підказка й варіанти — одним блоком по центру між верхнім рядком і «Пропустити».
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                TeperaOnboardingTitle(text = stringResource(R.string.online_estimate_onboarding_title))
                Text(
                    text = stringResource(R.string.online_estimate_onboarding_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DailyOnlineGuess.entries.forEach { guess ->
                        GlassRow(
                            label = dailyGuessLabel(guess),
                            cornerRadius = 24.dp,
                            onClick = { finish(guess) },
                            leading = { TeperaIconCircle(icon = TeperaSymbols.Schedule) },
                            trailing = {
                                Icon(
                                    TeperaSymbols.ChevronRight,
                                    contentDescription = null,
                                    tint = TeperaPalette.buttonBrandDark.copy(alpha = 0.5f)
                                )
                            }
                        )
                    }
                }
                }
                TeperaButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = { finish(null) },
                    type = TeperaButtonType.Tertiary
                )
            }
            OnboardingTopBar(
                step = 3,
                onSkipAll = onSkipAll,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}


@Composable
internal fun dailyGuessLabel(guess: DailyOnlineGuess): String = when (guess) {
    DailyOnlineGuess.UNDER_3H -> stringResource(R.string.online_estimate_range_under_3h)
    DailyOnlineGuess.THREE_TO_5H -> stringResource(R.string.online_estimate_range_3_5h)
    DailyOnlineGuess.OVER_5H -> stringResource(R.string.online_estimate_range_over_5h)
}
