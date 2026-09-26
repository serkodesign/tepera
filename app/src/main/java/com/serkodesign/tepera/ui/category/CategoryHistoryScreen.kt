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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.diary.HistoryEntryItem
import com.serkodesign.tepera.ui.diary.HistoryEntryRow
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
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Той самий вигляд, що у Щоденнику: заголовок доби (18sp Medium #003926), під ним картки записів
                    // ([HistoryEntryRow]) з проміжком 4dp; між добами 32dp.
                    state.groups.forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = formatDayLabel(group.dayStartMillis),
                                modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() },
                                color = TeperaPalette.buttonBrandDark,
                                fontFamily = TeperaPalette.headlineFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                lineHeight = 19.8.sp,
                                letterSpacing = 0.018.sp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                group.entries.forEach { entry ->
                                    HistoryEntryRow(
                                        item = HistoryEntryItem(
                                            entry = entry,
                                            category = category,
                                            seriesRange = entry.seriesId?.let(state.seriesRanges::get)
                                        ),
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
}

private fun formatDayLabel(dayStartMillis: Long): String =
    SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(dayStartMillis))
