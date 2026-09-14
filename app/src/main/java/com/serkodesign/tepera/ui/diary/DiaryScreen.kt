package com.serkodesign.tepera.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.util.roundToQuarterHour
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Щоденник" — за прямим запитом користувача перенесено сюди з екрана Статистики (там була
 * `HistoryCard`, окрема від `PeriodSelector`) на нову вкладку навбару (node 2146:320, Figma),
 * яка замінила неактивну заглушку "Незабаром". Сам зміст не змінився — "сьогодні"+"вчора",
 * лічильники розблокувань, час останнього використання (лише вчора), список записів з
 * редагуванням/видаленням через `AddEntryScreen` у режимі редагування.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    activityRepository: ActivityRepository,
    categoryRepository: CategoryRepository,
    balanceRepository: BalanceRepository,
    sleepWindowRepository: SleepWindowRepository,
    unlockRepository: UnlockRepository,
    pauseRepository: PauseRepository,
    onEditEntry: (String) -> Unit
) {
    val viewModel: DiaryViewModel = viewModel(
        factory = DiaryViewModel.Factory(
            activityRepository, categoryRepository, balanceRepository,
            sleepWindowRepository, unlockRepository, pauseRepository
        )
    )
    val state by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diary_screen_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HistoryCard(
                groups = state.history,
                unlockCountToday = state.unlockCountToday,
                unlockCountYesterday = state.unlockCountYesterday,
                lastPhoneUseYesterdayMillis = state.lastPhoneUseYesterdayMillis,
                onEditEntry = onEditEntry
            )
        }
    }
}

@Composable
private fun HistoryCard(
    groups: List<HistoryDayGroup>,
    unlockCountToday: Int?,
    unlockCountYesterday: Int?,
    lastPhoneUseYesterdayMillis: Long?,
    onEditEntry: (String) -> Unit
) {
    if (groups.isEmpty() && unlockCountToday == null && unlockCountYesterday == null && lastPhoneUseYesterdayMillis == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.home_no_entries_today))
        }
        return
    }

    val groupsByToday = groups.associateBy { it.isToday }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(true, false).forEach { isToday ->
                val group = groupsByToday[isToday]
                val unlockCount = if (isToday) unlockCountToday else unlockCountYesterday
                val lastPhoneUseMillis = if (isToday) null else lastPhoneUseYesterdayMillis
                if (group == null && unlockCount == null && lastPhoneUseMillis == null) return@forEach
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (isToday) R.string.stats_history_today else R.string.stats_history_yesterday),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    unlockCount?.let { count ->
                        Text(
                            stringResource(
                                if (isToday) R.string.stats_unlock_count_today_format else R.string.stats_unlock_count_yesterday_format,
                                count
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    lastPhoneUseMillis?.let { millis ->
                        Text(
                            stringResource(R.string.stats_last_phone_use_yesterday_format, formatClockTime(millis)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    group?.items?.forEach { item ->
                        HistoryEntryRow(item = item, onEdit = { onEditEntry(item.entry.id) })
                    }
                }
            }
        }
    }
}

private fun formatClockTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
private fun HistoryEntryRow(item: HistoryEntryItem, onEdit: () -> Unit) {
    val accentColor = categoryColor(item.category.colorHex)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val endMillis = item.entry.startTime + item.entry.durationMinutes * 60_000L
    val (hours, remainderMinutes) = roundToQuarterHour(item.entry.durationMinutes)
    val durationText = when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon(item.category.iconName), contentDescription = null, tint = accentColor)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(categoryDisplayName(item.category), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.history_entry_row_format,
                    timeFormat.format(Date(item.entry.startTime)),
                    timeFormat.format(Date(endMillis)),
                    durationText
                ),
                style = MaterialTheme.typography.bodySmall
            )
            if (!item.entry.note.isNullOrBlank()) {
                Text(
                    text = item.entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.stats_history_edit_action))
        }
    }
}
