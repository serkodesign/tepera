package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

// Кольори плиток — токени Figma "App concept" (node 192:726, "This week"): текст + фон 10%.
private val MovementText = Color(0xFF026813)
private val MovementFill = Color(0x1A026813)
private val ReadingText = Color(0xFF220D99)
private val ReadingFill = Color(0x1A65A5FF)
private val HobbyText = Color(0xFF750D99)
private val HobbyFill = Color(0x1AD765FF)
private val DayStartText = Color(0xFF986800)
private val DayStartFill = Color(0x26E9B12F)

private const val HOBBY_WINDOW_DAYS = 7

/**
 * "Цей тиждень" — третя сторінка горизонтального пейджера Home (Figma "App concept"
 * k6s4prQ9oK9x2uUvzHRghR, node 192:726, "This week"): заголовок + ⓘ + "×" і сітка плиток 2 колонки.
 * Плитки: Рух/спорт (кількість записів), Читання (кількість + сумарний час), Хобі ("N з 7 днів"
 * — за запитом користувача метрика з макета повернена; знаменник 7 = довжина вікна, орієнтира
 * користувача нема), "День зазвичай починається" (середня точка старту). Плитка без даних не
 * показується (незалогованість ніколи не подається як докір, FR-3.4-подібний принцип).
 * "×" — закриття, якщо прочитав, до наступної доби.
 */
@Composable
fun WeeklyDigestCard(state: WeeklyDigestUiState, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var showInfo by remember { mutableStateOf(false) }

    val tiles = buildList<@Composable RowScope.() -> Unit> {
        if (state.movementCount > 0) {
            add {
                DigestTile(label = stringResource(R.string.category_movement)) {
                    HomeTintChip(state.movementCount.toString(), MovementText, MovementFill)
                }
            }
        }
        if (state.readingCount > 0) {
            add {
                DigestTile(label = stringResource(R.string.category_reading)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        HomeTintChip(state.readingCount.toString(), ReadingText, ReadingFill)
                        HomeTintChip(
                            text = formatDuration(state.readingMinutes),
                            textColor = TeperaPalette.timeChipText,
                            fill = TeperaPalette.timeChipBackground,
                            borderColor = TeperaPalette.timeChipBorder
                        )
                    }
                }
            }
        }
        if (state.hobbyDays > 0) {
            add {
                DigestTile(label = stringResource(R.string.category_hobby)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        HomeTintChip(state.hobbyDays.toString(), HobbyText, HobbyFill)
                        Text(
                            stringResource(R.string.weekly_digest_hobby_between),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeCardTextPrimary
                        )
                        HomeTintChip(HOBBY_WINDOW_DAYS.toString(), HobbyText, HobbyFill)
                        val suffix = stringResource(R.string.weekly_digest_hobby_suffix)
                        if (suffix.isNotEmpty()) {
                            Text(suffix, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = HomeCardTextPrimary)
                        }
                    }
                }
            }
        }
        state.dayUsuallyStartsMinuteOfDay?.let { minuteOfDay ->
            add {
                DigestTile(label = stringResource(R.string.weekly_digest_day_start_label)) {
                    HomeTintChip(formatTimeOfDay(minuteOfDay), DayStartText, DayStartFill)
                }
            }
        }
    }

    HomeCardSurface(modifier = modifier, containerColor = TeperaPalette.homeCardFill) {
        HomeCardTitleRow(
            title = stringResource(R.string.weekly_digest_card_title),
            onInfo = { showInfo = true },
            onDismiss = onDismiss
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            tiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowTiles.forEach { tile -> tile() }
                    if (rowTiles.size < 2) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
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

/** Біла плитка (радіус 8, паддінг 8): підпис 12sp зверху, плашки значень знизу. */
@Composable
private fun RowScope.DigestTile(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(TeperaPalette.cardActive)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, fontSize = 12.sp, color = HomeCardTextSecondary, maxLines = 1)
        content()
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
