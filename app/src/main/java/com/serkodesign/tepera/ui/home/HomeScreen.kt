package com.serkodesign.tepera.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.util.DayPeriod
import com.serkodesign.tepera.util.currentDayPeriod

/**
 * Стиль Home перенесений з Figma-фрейму "Everyday_Designs" (node 1930:233, Фаза 6): м'який
 * градієнтний фон, привітання за часом доби замість статичного заголовка, "Life balance" — два
 * пропорційні блоки Offline/Online (BalanceCard.kt) замість тонкого бару, картки категорій без
 * колонки Card/TopAppBar/FAB — плюс переїхав у "таблетку" нижнього навбару (TeperaNavHost), а
 * Налаштування відкриваються через іконку-шестерню тут, а не окремою вкладкою навбару.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    balanceRepository: BalanceRepository,
    settingsStore: SettingsStore,
    activeTimerStore: ActiveTimerStore,
    onOpenSettings: () -> Unit,
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

    // Прозорий containerColor: градієнтний фон малює зовнішній Box у TeperaNavHost (а не тут) —
    // інакше він потрапляє під contentPadding зовнішнього Scaffold і не сягає країв екрана.
    Scaffold(containerColor = Color.Transparent) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                HomeHeader(onOpenSettings = onOpenSettings)

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.life_balance_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = TeperaPalette.headlineFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    IconButton(onClick = onShowOnboarding, modifier = Modifier.size(20.dp)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.usage_access_learn_more)
                        )
                    }
                }

                LifeBalanceSection(
                    state = balanceState,
                    onOpenUsageAccessSettings = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onLearnMore = onShowOnboarding,
                    modifier = Modifier.padding(16.dp)
                )

                Text(
                    text = stringResource(R.string.activities_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    val period = remember { currentDayPeriod() }
    val greetingRes = when (period) {
        DayPeriod.MORNING -> R.string.greeting_morning
        DayPeriod.DAY -> R.string.greeting_day
        DayPeriod.EVENING -> R.string.greeting_evening
        DayPeriod.NIGHT -> R.string.greeting_night
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(greetingRes),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp
            )
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_nav_action))
        }
    }
}

/**
 * Тап починає/зупиняє живий таймер для цієї категорії (HomeViewModel.toggleTimer). Стиль картки —
 * з Figma-фрейму: іконка категорії (тонована власним кольором категорії, без кружка-підложки),
 * play/pause замість тексту з часом (за запитом прибрано і живий лічильник, і статичний підпис
 * хвилин — лишились лише іконка й назва).
 */
@Composable
private fun CategoryCard(item: CategoryTodaySummary, onClick: () -> Unit) {
    val isTracking = item.trackingStartTime != null
    val accentColor = categoryColor(item.category.colorHex)
    val containerColor = if (isTracking) TeperaPalette.cardActive else TeperaPalette.cardTranslucent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(
                imageVector = categoryIcon(item.category.iconName),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryDisplayName(item.category),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = if (isTracking) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
