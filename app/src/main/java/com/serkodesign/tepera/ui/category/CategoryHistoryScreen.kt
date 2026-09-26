package com.serkodesign.tepera.ui.category

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.diary.seriesRangeText
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.roundToQuarterHour
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Тап по тілу картки категорії на Home (не по кнопках таймера/додавання часу) — повна історія
 * записів САМЕ цієї категорії, без обмеження "сьогодні+вчора" (на відміну від "Щоденника"), бо
 * список уже звужений однією категорією і не стає непридатно довгим.
 */
@Composable
fun CategoryHistoryScreen(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    categoryId: String,
    onEditEntry: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: CategoryHistoryViewModel = viewModel(
        factory = CategoryHistoryViewModel.Factory(categoryRepository, activityRepository, categoryId)
    )
    val state by viewModel.uiState.collectAsState()
    val category = state.category

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(
                title = category?.let { categoryDisplayName(it) }
                    ?: stringResource(R.string.category_history_screen_title_fallback),
                onBack = onBack
            )
            if (category == null || state.groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.category_history_empty))
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.groups.forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = formatDayLabel(group.dayStartMillis),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            group.entries.forEach { entry ->
                                CategoryHistoryEntryRow(
                                    entry = entry,
                                    category = category,
                                    seriesRange = entry.seriesId?.let(state.seriesRanges::get),
                                    onEdit = { onEditEntry(entry.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDayLabel(dayStartMillis: Long): String =
    SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(dayStartMillis))

@Composable
private fun CategoryHistoryEntryRow(
    entry: ActivityEntryEntity,
    category: CategoryEntity,
    seriesRange: Pair<Long, Long>?,
    onEdit: () -> Unit
) {
    val accentColor = categoryColor(category.colorHex)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val endMillis = entry.startTime + entry.durationMinutes * 60_000L
    val (hours, remainderMinutes) = roundToQuarterHour(entry.durationMinutes)
    val durationText = when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.cardTranslucent)
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon(category.iconName), contentDescription = null, tint = categoryGlyphColor(accentColor))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(
                    R.string.history_entry_row_format,
                    timeFormat.format(Date(entry.startTime)),
                    timeFormat.format(Date(endMillis)),
                    durationText
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            seriesRange?.let { range ->
                Text(
                    text = seriesRangeText(range),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!entry.note.isNullOrBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
