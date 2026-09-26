package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour

// Кольори чисел у плитках — ті самі відтінки, що були в плашках Figma (node 192:726, "This week"), тепер як
// колір великого числа на білій плитці (усі ≥ 4.5:1 до білого).
private val MovementText = Color(0xFF026813)
private val ReadingText = Color(0xFF220D99)
private val HobbyText = Color(0xFF750D99)
private val DayStartText = Color(0xFF986800)

private const val HOBBY_WINDOW_DAYS = 7

/** Одна плитка: підпис, головне число (великий шрифт) і необов'язкове пояснення поруч (приглушене). */
private class DigestItem(val label: String, val main: String, val mainColor: Color, val note: String? = null)

/**
 * "Цей тиждень" — третя сторінка горизонтального пейджера Home (Figma "App concept"
 * k6s4prQ9oK9x2uUvzHRghR, node 192:726, "This week"): заголовок + ⓘ + "×" і сітка плиток 2 колонки.
 * Плитки: Рух/спорт (кількість записів), Читання (кількість + сумарний час), Хобі ("N з 7 днів"
 * — за запитом користувача метрика з макета повернена; знаменник 7 = довжина вікна, орієнтира
 * користувача нема), "День зазвичай починається" (середня точка старту). Плитка без даних не
 * показується (незалогованість ніколи не подається як докір, FR-3.4-подібний принцип).
 * Кнопки закриття "×" нема (за запитом користувача).
 *
 * Розкладка під спільну висоту карток пейджера: ряди плиток ділять усю вільну висоту порівну
 * (`weight(1f)`), а всередині плитки підпис зверху й число знизу — на вищій картці плитки просто
 * вищі, а не з порожнечею під вмістом. Число — великим шрифтом замість дрібних кольорових плашок.
 */
@Composable
fun WeeklyDigestCard(state: WeeklyDigestUiState, modifier: Modifier = Modifier) {
    var showInfo by remember { mutableStateOf(false) }

    val items = buildList {
        if (state.movementCount > 0) {
            add(DigestItem(stringResource(R.string.category_movement), state.movementCount.toString(), MovementText))
        }
        if (state.readingCount > 0) {
            add(
                DigestItem(
                    stringResource(R.string.category_reading), state.readingCount.toString(), ReadingText,
                    note = "· " + formatDuration(state.readingMinutes)
                )
            )
        }
        if (state.hobbyDays > 0) {
            val between = stringResource(R.string.weekly_digest_hobby_between)
            val suffix = stringResource(R.string.weekly_digest_hobby_suffix)
            add(
                DigestItem(
                    stringResource(R.string.category_hobby), state.hobbyDays.toString(), HobbyText,
                    note = listOf(between, HOBBY_WINDOW_DAYS.toString(), suffix).filter { it.isNotEmpty() }.joinToString(" ")
                )
            )
        }
        state.dayUsuallyStartsMinuteOfDay?.let { minuteOfDay ->
            add(DigestItem(stringResource(R.string.weekly_digest_day_start_label), formatTimeOfDay(minuteOfDay), DayStartText))
        }
    }

    HomeCardSurface(modifier = modifier, containerColor = TeperaPalette.homeCardFill) {
        HomeCardTitleRow(
            title = stringResource(R.string.weekly_digest_card_title),
            onInfo = { showInfo = true }
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowItems.forEach { item -> DigestTile(item, Modifier.weight(1f).fillMaxHeight()) }
                    if (rowItems.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (showInfo) {
        HomeInfoDialog(
            title = stringResource(R.string.weekly_digest_card_title),
            body = stringResource(R.string.weekly_digest_info_body),
            onDismiss = { showInfo = false }
        )
    }
}

/** Біла плитка (радіус 16, паддінг 12): підпис 14sp зверху, число 24sp + пояснення знизу. */
@Composable
private fun DigestTile(item: DigestItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardActive)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(item.label, style = MaterialTheme.typography.labelLarge, color = HomeCardTextSecondary, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                text = item.main,
                color = item.mainColor,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                maxLines = 1
            )
            item.note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomeCardTextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun formatDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}

private fun formatTimeOfDay(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
