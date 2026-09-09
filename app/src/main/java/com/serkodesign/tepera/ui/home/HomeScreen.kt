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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
 * Стиль Home перенесений з Figma-фрейму "Everyday_Designs" (сторінка "Tepera", node 1951:4017,
 * оновлений мокап — Фаза 6): "Life balance" тепер ОДНА картка-обгортка (заголовок + бари разом),
 * сітка категорій 2x3 (було 3x2) з більшими картками, і КОЖНА картка має ДВІ окремі кнопки
 * знизу — широку play/pause (тап-таймер, як і раніше) і окрему кнопку "more_time" (годинник із
 * плюсом) для РУЧНОГО додавання часу САМЕ до цієї категорії. За запитом користувача це замінює
 * загальну кнопку "+" (яка раніше відкривала Add Entry з вибором категорії зі списку) — тепер
 * такого загального входу з Home більше нема, лише per-категорійний "add time".
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
    onAddEntryForCategory: (String) -> Unit,
    onShowOnboarding: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(categoryRepository, activityRepository, activeTimerStore)
    )
    val summary by viewModel.todaySummary.collectAsState()

    val balanceViewModel: BalanceViewModel = viewModel(
        factory = BalanceViewModel.Factory(balanceRepository, activityRepository, categoryRepository, settingsStore)
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

            // "My day" (SRS v2.5, розділ 4.4) — ОДНА картка-обгортка (заголовок+шкала+легенда
            // разом), а не окремий заголовок над секцією без фону, як було раніше.
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(TeperaPalette.cardTranslucentLight)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MyDaySection(
                    state = balanceState,
                    onOpenUsageAccessSettings = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onLearnMore = onShowOnboarding
                )
            }

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
                // 2x3 сітка категорій (було 3x2): до 5 дефолтних + 1 кастомна (FR-2.1/2.2)
                // точно заповнюють 2 колонки на 3 ряди. Раніше NavHost резервував під навбар-
                // "таблетку" фіксовану висоту зверху від Scaffold, і ЦЕЙ екран мав ще й власний
                // Scaffold-inset поверх — подвійний нижній відступ стискав сітку так, що останній
                // ряд карток обрізався. NavHost більше не резервує нижній відступ для цього
                // екрана (TeperaNavHost.kt), тож тут потрібен власний bottomNavBarHeight-запас, щоб
                // картки за замовчуванням лишались НАД "таблеткою" — а якщо не влазять, останній
                // ряд природно йде під напівпрозору "таблетку" при прокрутці (за запитом користувача).
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summary, key = { it.category.id }) { item ->
                        CategoryCard(
                            item = item,
                            onToggleTimer = { viewModel.toggleTimer(item.category.id) },
                            onAddTime = { onAddEntryForCategory(item.category.id) }
                        )
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
 * Картка категорії (Home screen, node 1951:4017): іконка+назва зверху, ДВІ кнопки знизу —
 * широка play/pause (тап-таймер, HomeViewModel.toggleTimer) і окрема "add time" (Icons.Filled.
 * MoreTime — той самий глиф, що "more_time" у фреймі) для ручного додавання часу САМЕ цій
 * категорії (AddEntryScreen з попередньо вибраною категорією, ADD_ENTRY_WITH_CATEGORY).
 */
@Composable
private fun CategoryCard(
    item: CategoryTodaySummary,
    onToggleTimer: () -> Unit,
    onAddTime: () -> Unit
) {
    val isTracking = item.trackingStartTime != null
    val accentColor = categoryColor(item.category.colorHex)
    val containerColor = if (isTracking) TeperaPalette.cardActive else TeperaPalette.cardTranslucent
    val displayName = categoryDisplayName(item.category)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(containerColor)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(item.category.iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isTracking) TeperaPalette.brandAccent else TeperaPalette.cardActive)
                    .clickable(onClick = onToggleTimer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isTracking) R.string.category_stop_action else R.string.category_start_action
                    ),
                    tint = if (isTracking) Color.White else Color.Black
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(TeperaPalette.cardActive)
                    .clickable(onClick = onAddTime),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreTime,
                    contentDescription = stringResource(R.string.add_time_action_format, displayName),
                    tint = Color.Black
                )
            }
        }
    }
}
