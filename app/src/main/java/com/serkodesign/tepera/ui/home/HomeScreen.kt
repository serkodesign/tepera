package com.serkodesign.tepera.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    balanceRepository: BalanceRepository,
    settingsStore: SettingsStore,
    activeTimerStore: ActiveTimerStore,
    onAddEntry: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(categoryRepository, activityRepository, activeTimerStore)
    )
    val summary by viewModel.todaySummary.collectAsState()

    val balanceViewModel: BalanceViewModel = viewModel(
        factory = BalanceViewModel.Factory(balanceRepository, activityRepository, settingsStore)
    )
    val balanceState by balanceViewModel.uiState.collectAsState()

    // Доступ до статистики використання надається в системних Налаштуваннях, поза застосунком —
    // без цього ефекту повернення з Налаштувань не оновило б картку без ручного перезаходу на Home.
    LifecycleResumeEffect(Unit) {
        balanceViewModel.refresh()
        onPauseOrDispose { }
    }

    val onboardingSeen by settingsStore.onboardingUsageAccessSeen.collectAsState(initial = true)
    LaunchedEffect(balanceState.hasUsageAccess, onboardingSeen) {
        if (balanceState.hasUsageAccess == false && !onboardingSeen) {
            onShowOnboarding()
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_screen_title)) },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(
                            Icons.Filled.BarChart,
                            contentDescription = stringResource(R.string.stats_nav_action)
                        )
                    }
                    IconButton(onClick = onOpenCategories) {
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = stringResource(R.string.categories_nav_action)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_nav_action)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.add_entry_button),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            BalanceCard(
                state = balanceState,
                onOpenUsageAccessSettings = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
                onLearnMore = onShowOnboarding,
                modifier = Modifier.padding(16.dp)
            )

            if (summary.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.home_no_entries_today))
                }
            } else {
                // 3x2 сітка категорій: до 5 дефолтних + 1 кастомна (FR-2.1/2.2) точно
                // заповнюють 3 колонки на 2 ряди, без порожніх чи переповнених рядків.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summary, key = { it.category.id }) { item ->
                        CategoryCard(item = item, onClick = { viewModel.toggleTimer(item.category.id) })
                    }
                }
            }
        }
    }
}

/**
 * Тап починає/зупиняє живий таймер для цієї категорії (HomeViewModel.toggleTimer). Поки таймер
 * іде — кольорова рамка й іконка "стоп" замість категорійної, плюс лічильник, що цокає щосекунди.
 */
@Composable
private fun CategoryCard(item: CategoryTodaySummary, onClick: () -> Unit) {
    val isTracking = item.trackingStartTime != null
    val accentColor = categoryColor(item.category.colorHex)
    val defaultContainerColor = CardDefaults.cardColors().containerColor
    val containerColor = if (isTracking) {
        lerp(defaultContainerColor, accentColor, 0.15f)
    } else {
        defaultContainerColor
    }

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isTracking) BorderStroke(2.dp, accentColor) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isTracking) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface
                    )
                } else {
                    Icon(
                        imageVector = categoryIcon(item.category.iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
            Text(
                text = categoryDisplayName(item.category),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isTracking) {
                ElapsedTimeText(
                    startTime = item.trackingStartTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.minutes_short_format, item.minutesToday),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/** Свідомий виняток із "без live-таймера" (FR-4.2 стосується лише віджета) — тут це сама суть дії. */
@Composable
private fun ElapsedTimeText(startTime: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startTime) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsedSeconds = ((now - startTime) / 1000).coerceAtLeast(0)
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val text = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
    Text(text, style = style, color = color, modifier = modifier)
}
