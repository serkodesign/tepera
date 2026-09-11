package com.serkodesign.tepera.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FR-D.1–D.5 (SRS v2.6): картка виявлених пауз, інлайн на Home — той самий "скляний" стиль, що
 * WeeklyReflectionCard. Компактна (FR-D.11): заголовок і по одному рядку на паузу. Категорія
 * обирається в діалозі за тапом на рядок — не окремими іконками в рядку, щоб рядок лишався одним
 * компактним рядком незалежно від кількості категорій.
 * **FR-D.6 (SRS v2.8):** метрика "твій день з телефоном" (межі першої/останньої сесії) прибрана —
 * назва бреше (проміжок читається як час використання, хоча включає й кишеню), число велике без
 * важеля впливу. Межі дня тепер видно самі собою на тепловому патерні (`PatternMiniCard`/
 * `HourlyHeatStrip`), без окремого числа.
 * `.animateContentSize()` на картці — щоб рядок паузи, що зникає (позначено/пропущено), плавно
 * стискав картку, а не миттєво "вирізав" шматок LayoutNode (той самий артефакт-баг, що й у
 * ContextCardStack.kt, лише в мініатюрі — на рівні одного рядка всередині картки).
 */
@Composable
fun PauseCard(
    state: PauseCardUiState,
    categories: List<CategoryEntity>,
    onLabel: (PauseUiGap, String) -> Unit,
    onDismissGap: (PauseUiGap) -> Unit,
    onDismissCard: () -> Unit
) {
    var gapForPicker by remember { mutableStateOf<PauseUiGap?>(null) }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.cardTranslucentLight)
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Закриття ЦІЛОЇ картки (за прямим запитом користувача, не в SRS) — окремо від per-gap
        // "×" нижче, який пропускає лише одну паузу назавжди. Тут — "прочитав, ховай до наступного
        // вікна опитування" (FR-D.3), непозначені паузи лишаються в БД.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismissCard, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.context_card_dismiss_action),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (state.gaps.isNotEmpty()) {
            Text(
                text = stringResource(R.string.pause_card_title),
                style = MaterialTheme.typography.bodyMedium
            )
            state.gaps.forEach { gap ->
                PauseGapRow(
                    gap = gap,
                    onClick = { gapForPicker = gap },
                    onDismiss = { onDismissGap(gap) }
                )
            }
        }
    }

    val pickerGap = gapForPicker
    if (pickerGap != null) {
        CategoryPickerDialog(
            categories = categories,
            onPick = { categoryId ->
                onLabel(pickerGap, categoryId)
                gapForPicker = null
            },
            onDismiss = { gapForPicker = null }
        )
    }
}

@Composable
private fun PauseGapRow(gap: PauseUiGap, onClick: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucent)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.pause_gap_row_format,
                formatTime(gap.startTime),
                formatTime(gap.startTime + gap.durationMinutes * 60_000L),
                formatDuration(gap.durationMinutes)
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.pause_gap_dismiss_action)
            )
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<CategoryEntity>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pause_pick_category_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { category ->
                    val accentColor = categoryColor(category.colorHex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(category.id) }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon(category.iconName),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(categoryDisplayName(category))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pause_pick_category_cancel)) }
        }
    )
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
private fun formatDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}
