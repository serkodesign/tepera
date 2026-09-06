package com.serkodesign.tepera.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    balanceRepository: BalanceRepository,
    settingsStore: SettingsStore,
    onAddEntry: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(categoryRepository, activityRepository)
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
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summary, key = { it.category.id }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(categoryColor(item.category.colorHex), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon(item.category.iconName),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.surface
                                        )
                                    }
                                    Text(
                                        text = categoryDisplayName(item.category),
                                        modifier = Modifier.padding(start = 12.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.minutes_short_format, item.minutesToday),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
