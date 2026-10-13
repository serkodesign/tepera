package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.onboarding.DailyOnlineGuess
import com.serkodesign.tepera.ui.onboarding.dailyGuessLabel
import com.serkodesign.tepera.ui.theme.TeperaEstimateBar
import com.serkodesign.tepera.ui.theme.TeperaHourAxis
import com.serkodesign.tepera.ui.theme.teperaEstimateScaleHours
import com.serkodesign.tepera.util.roundToQuarterHour

/**
 * T-3 (tepera-dev-spec.md), розділ 2.2: "два числа поруч" — смуга "Твоя оцінка" показує
 * ОРИГІНАЛЬНИЙ текст діапазону (не перетворену цифру — людина обирала діапазон, не число),
 * "Насправді" — реальні хвилини. Без жодного слова-оцінки (FR-P.6): лише дві смуги на спільній
 * шкалі годин — діапазон оцінки (штриховий контур) і факт (заливка). [ContextCardHeader] закриває
 * картку назавжди для цього розкриття (`OnlineEstimateRevealViewModel.dismiss()`).
 *
 * Смуги й вісь — компоненти дизайн-системи [TeperaEstimateBar]/[TeperaHourAxis] (Figma "Assets" →
 * секція "Estimate card" → `Estimate card`, Scale=6h/12h). Тут лише дані: діапазон оцінки, шкала.
 */
@Composable
fun OnlineEstimateRevealCard(state: OnlineEstimateRevealUiState, onDismiss: () -> Unit) {
    val guess = DailyOnlineGuess.fromRepresentativeMinutes(state.estimatedMinutes)
    val guessText = guess?.let { dailyGuessLabel(it) } ?: formatDuration(state.estimatedMinutes)
    val actualText = formatDuration(state.actualMinutes)
    val guessLabel = stringResource(R.string.weekly_reflection_your_guess_label)
    val actualLabel = stringResource(R.string.weekly_reflection_actual_label)

    val actualMinutes = state.actualMinutes.toInt().coerceAtLeast(0)
    val scaleHours = teperaEstimateScaleHours(actualMinutes)
    val scaleMinutes = scaleHours * 60
    val guessRange = if (guess != null) guessRangeMinutes(state.estimatedMinutes, scaleMinutes) else null

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .contextInfoSurface()
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$guessLabel: $guessText. $actualLabel: $actualText"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContextCardHeader(title = stringResource(R.string.online_estimate_reveal_title), onDismiss = onDismiss)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TeperaEstimateBar(label = guessLabel, value = guessText, scaleMinutes = scaleMinutes, range = guessRange)
                TeperaEstimateBar(label = actualLabel, value = actualText, scaleMinutes = scaleMinutes, fillMinutes = actualMinutes)
            }
            TeperaHourAxis(scaleHours = scaleHours)
        }
    }
}

/**
 * Діапазон оцінки в хвилинах за "представницьким" значенням (межі з рядків "До 3 год" / "3–5 год" /
 * "Понад 5 год"). Останній діапазон тягнеться до кінця шкали.
 */
private fun guessRangeMinutes(representativeMinutes: Long, scaleMinutes: Int): IntRange = when {
    representativeMinutes < 180 -> 0..180
    representativeMinutes < 300 -> 180..300
    else -> 300..scaleMinutes
}

@Composable
private fun formatDuration(minutes: Long): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes.toInt())
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}
