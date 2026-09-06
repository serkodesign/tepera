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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${stringResource(R.string.balance_online_label)}: " +
                                stringResource(R.string.minutes_short_format, state.onlineMinutes)
                        )
                        Text(
                            "${stringResource(R.string.balance_offline_label)}: " +
                                stringResource(R.string.minutes_short_format, state.offlineMinutes)
                        )
                    }
                    BalanceBar(
                        onlineRatio = state.onlineMinutes.toFloat() / state.denominatorMinutes.coerceAtLeast(1),
                        targetRatio = state.targetMinutes.toFloat() / state.denominatorMinutes.coerceAtLeast(1)
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

/** FR-3.4: горизонтальна шкала Online-ratio з вертикальною засічкою на позиції таргету. */
@Composable
private fun BalanceBar(onlineRatio: Float, targetRatio: Float, modifier: Modifier = Modifier) {
    val clampedOnline = onlineRatio.coerceIn(0f, 1f)
    val clampedTarget = targetRatio.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier.fillMaxWidth().height(12.dp)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = corner)
        if (clampedOnline > 0f) {
            drawRoundRect(
                color = fillColor,
                size = size.copy(width = size.width * clampedOnline),
                cornerRadius = corner
            )
        }
        val markerX = size.width * clampedTarget
        drawLine(
            color = markerColor,
            start = Offset(markerX, 0f),
            end = Offset(markerX, size.height),
            strokeWidth = 3.dp.toPx()
        )
    }
}
