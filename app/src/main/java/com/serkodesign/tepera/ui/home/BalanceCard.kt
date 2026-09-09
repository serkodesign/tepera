package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour

/**
 * FR-3.1–3.12, FR-5.1 (SRS v2.5): "Мій день" — стиль з референсного макета (розділ 4.4,
 * Figma node 2002:170): заголовок "Твій день триває X" (FR-3.7, росте разом з реальним часом,
 * НЕ фіксована доба), тиха багатосегментна шкала (Online + кожна залогована сьогодні категорія
 * своїм кольором + нейтральна "Решта дня") з тихою засічкою орієнтиру БЕЗ підпису (FR-3.10),
 * і легенда під шкалою — кольоровий квадрат + назва + час, без відсотків (FR-P.6). Свідомо НЕ
 * протиставлення Online/Offline на одній шкалі (FR-3.8 — різні джерела, різна природа підрахунку).
 */
@Composable
fun MyDaySection(
    state: BalanceUiState,
    onOpenUsageAccessSettings: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // FR-3.7: заголовок росте разом з реальним часом — не показуємо "24 год" чи будь-яку
        // фіксовану абстракцію доби, лише скільки дня вже сталося.
        Text(
            text = stringResource(R.string.my_day_title_format, formatBalanceDuration(state.dayLengthMinutes)),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onLearnMore, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.usage_access_learn_more))
        }
    }

    when (state.hasUsageAccess) {
        null -> Unit // перевірка ще триває, секція мовчить, щоб не блимати fallback-текстом
        false -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.usage_access_prompt_title), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.usage_access_prompt_body), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onLearnMore) { Text(stringResource(R.string.usage_access_learn_more)) }
                        Button(onClick = onOpenUsageAccessSettings) { Text(stringResource(R.string.usage_access_open_settings)) }
                    }
                }
            }
        }
        true -> {
            val segments = daySegments(state)
            if (segments.isNotEmpty()) {
                DayStructureBar(segments = segments, targetMinutes = state.targetMinutes, dayLengthMinutes = state.dayLengthMinutes)
                DayStructureLegend(segments = segments)
            }
        }
    }
}

private data class DaySegment(val label: String, val color: Color, val minutes: Int)

/**
 * FR-3.3, FR-3.4: порядок — Online, потім кожна категорія з ненульовим часом сьогодні (своїм
 * кольором), потім нейтральна "Решта дня" останньою (референсний макет, розділ 4.4).
 */
@Composable
private fun daySegments(state: BalanceUiState): List<DaySegment> {
    val onlineLabel = stringResource(R.string.balance_online_label)
    val restLabel = stringResource(R.string.balance_rest_of_day_label)
    return buildList {
        if (state.onlineMinutes > 0) add(DaySegment(onlineLabel, TeperaPalette.onlineCard, state.onlineMinutes))
        state.categorySegments.forEach {
            add(DaySegment(categoryDisplayName(it.category), categoryColor(it.category.colorHex), it.minutes))
        }
        if (state.restOfDayMinutes > 0) add(DaySegment(restLabel, TeperaPalette.restOfDayCard, state.restOfDayMinutes))
    }
}

@Composable
private fun DayStructureBar(segments: List<DaySegment>, targetMinutes: Int, dayLengthMinutes: Int) {
    // FR-3.10: тиха засічка орієнтиру — тонка вертикальна лінія, БЕЗ підпису, ніколи не
    // змінює колір при перевищенні (розділ 4.3–4.4 SRS: "якщо з'явиться спокуса підсвітити
    // перевищення кольором — це сигнал звірити рішення з розділом 4, не з інтуїцією"). Позиція —
    // частка від max(довжина дня, орієнтир), щоб лишатись у межах шкали незалежно від того,
    // досягнутий орієнтир чи ще ні.
    val referenceMinutes = maxOf(dayLengthMinutes, targetMinutes, 1)
    val markerFraction = (targetMinutes.toFloat() / referenceMinutes).coerceIn(0f, 1f)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val markerX = maxWidth * markerFraction
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            segments.forEach { segment ->
                Box(
                    modifier = Modifier
                        .weight(segment.minutes.coerceAtLeast(1).toFloat())
                        .fillMaxHeight()
                        .background(segment.color)
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = markerX)
                .width(1.dp)
                .height(16.dp)
                .background(Color.Black.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun DayStructureLegend(segments: List<DaySegment>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        segments.forEach { segment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(segment.color))
                    Text(segment.label, style = MaterialTheme.typography.bodyMedium)
                }
                // FR-P.6: час завжди поруч із назвою, ніколи голий відсоток самотужки.
                Text(formatBalanceDuration(segment.minutes), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Округлення до 15 хв (за запитом користувача, замість "245 хв") — напр. "3 год 45 хв",
 * а не "3 год 47 хв".
 */
@Composable
private fun formatBalanceDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}
