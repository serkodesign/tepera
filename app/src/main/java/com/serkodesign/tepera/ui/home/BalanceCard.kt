package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R

/** FR-3.1–3.6: Online/Offline баланс — fallback без дозволу, інакше шкала з маркером таргету. */
@Composable
fun BalanceCard(
    state: BalanceUiState,
    onOpenUsageAccessSettings: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.balance_card_title), style = MaterialTheme.typography.titleMedium)

            when (state.hasUsageAccess) {
                null -> Unit // перевірка ще триває, картка мовчить, щоб не блимати fallback-текстом
                false -> {
                    Text(
                        stringResource(R.string.usage_access_prompt_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        stringResource(R.string.usage_access_prompt_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onLearnMore) {
                            Text(stringResource(R.string.usage_access_learn_more))
                        }
                        Button(onClick = onOpenUsageAccessSettings) {
                            Text(stringResource(R.string.usage_access_open_settings))
                        }
                    }
                }
                true -> {
                    // Два окремі пропорційні бари (Online/Offline) замість однієї шкали —
                    // інформативніше, ніж один bar, що ніяк не показував Offline-хвилини візуально.
                    // Спільний максимум для обох барів, щоб їхня довжина була порівнюваною між собою.
                    val maxScale = maxOf(state.onlineMinutes, state.offlineMinutes, state.targetMinutes, 1)
                    ProportionalBar(
                        label = stringResource(R.string.balance_online_label),
                        valueText = stringResource(R.string.minutes_short_format, state.onlineMinutes),
                        ratio = state.onlineMinutes.toFloat() / maxScale,
                        targetRatio = state.targetMinutes.toFloat() / maxScale,
                        barColor = MaterialTheme.colorScheme.primary
                    )
                    ProportionalBar(
                        label = stringResource(R.string.balance_offline_label),
                        valueText = stringResource(R.string.minutes_short_format, state.offlineMinutes),
                        ratio = state.offlineMinutes.toFloat() / maxScale,
                        targetRatio = null,
                        barColor = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        stringResource(R.string.balance_target_format, state.targetMinutes),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Один пропорційний бар з підписом і значенням хвилин над ним; targetRatio — необов'язкова
 * вертикальна засічка (таргет стосується лише Online-часу, FR-3.4, тож Offline-бар її не має).
 */
@Composable
private fun ProportionalBar(
    label: String,
    valueText: String,
    ratio: Float,
    targetRatio: Float?,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val clampedRatio = ratio.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.error

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
            val corner = CornerRadius(size.height / 2)
            drawRoundRect(color = trackColor, cornerRadius = corner)
            if (clampedRatio > 0f) {
                drawRoundRect(
                    color = barColor,
                    size = size.copy(width = size.width * clampedRatio),
                    cornerRadius = corner
                )
            }
            if (targetRatio != null) {
                val markerX = size.width * targetRatio.coerceIn(0f, 1f)
                drawLine(
                    color = markerColor,
                    start = Offset(markerX, 0f),
                    end = Offset(markerX, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}
