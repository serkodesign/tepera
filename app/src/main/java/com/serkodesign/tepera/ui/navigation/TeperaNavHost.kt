package com.serkodesign.tepera.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BackupRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.ui.addentry.AddEntryScreen
import com.serkodesign.tepera.ui.category.CategoriesScreen
import com.serkodesign.tepera.ui.home.HomeScreen
import com.serkodesign.tepera.ui.onboarding.OnboardingScreen
import com.serkodesign.tepera.ui.settings.ExclusionListScreen
import com.serkodesign.tepera.ui.settings.SettingsScreen
import com.serkodesign.tepera.ui.stats.StatsScreen
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.teperaGradientBackground

private object Routes {
    const val HOME = "home"
    const val ADD_ENTRY = "add_entry"
    const val ADD_ENTRY_WITH_CATEGORY = "add_entry?categoryId={categoryId}"
    const val CATEGORIES = "categories"
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
    const val EXCLUSION_LIST = "exclusion_list"
    const val STATS = "stats"

    // Дві вкладки нижнього навбару (Figma-фрейм Everyday_Designs, Фаза 6): Home і Статистика.
    // Кнопка "+" всередині тієї самої "таблетки" — не окрема вкладка, а одноразовий перехід на
    // Add Entry. Налаштування (раніше третя вкладка "Меню") тепер відкриваються іконкою-шестернею
    // на самому Home, як у фреймі — це вже НЕ вкладка навбару.
    val BOTTOM_NAV_ROUTES = setOf(HOME, STATS)

    fun addEntry(categoryId: String? = null) =
        if (categoryId != null) "add_entry?categoryId=$categoryId" else ADD_ENTRY
}

@Composable
fun TeperaNavHost(
    categoryRepository: CategoryRepository,
    activityRepository: ActivityRepository,
    balanceRepository: BalanceRepository,
    excludedAppRepository: ExcludedAppRepository,
    installedAppsProvider: InstalledAppsProvider,
    settingsStore: SettingsStore,
    activeTimerStore: ActiveTimerStore,
    backupRepository: BackupRepository,
    navController: NavHostController = rememberNavController(),
    // FR-4.1/4.4: тап по кнопці категорії на віджеті або по Quick Settings tile відкриває
    // MainActivity з цим "натяком" — обробляється один раз при вході, не при кожній рекомпозиції.
    pendingOpenAddEntry: Boolean = false,
    pendingCategoryId: String? = null
) {
    LaunchedEffect(Unit) {
        if (pendingOpenAddEntry) {
            navController.navigate(Routes.addEntry(pendingCategoryId))
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Градієнт застосовується ТУТ, на самому зовнішньому Box (а не всередині HomeScreen/
    // StatsScreen) — інакше він потрапляє під contentPadding зовнішнього Scaffold і не сягає
    // країв екрана (status bar/навбар лишаються білою смугою поверх, підтверджено на
    // Samsung S23). Умовний, не глобальний: Налаштування/Категорії/Додати активність — досі
    // дефолтна Material 3 тема, для них Figma-дизайну ще нема.
    val useGradientBackground = currentRoute in Routes.BOTTOM_NAV_ROUTES
    Box(
        modifier = if (useGradientBackground) {
            Modifier.teperaGradientBackground()
        } else {
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (currentRoute in Routes.BOTTOM_NAV_ROUTES) {
                    TeperaBottomNavBar(currentRoute = currentRoute, navController = navController)
                }
            }
        ) { scaffoldPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(scaffoldPadding)
            ) {
            composable(Routes.HOME) {
                HomeScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    balanceRepository = balanceRepository,
                    settingsStore = settingsStore,
                    activeTimerStore = activeTimerStore,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onShowOnboarding = { navController.navigate(Routes.ONBOARDING) }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    balanceRepository = balanceRepository
                )
            }
            composable(Routes.ADD_ENTRY) {
                AddEntryScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Routes.ADD_ENTRY_WITH_CATEGORY,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { entry ->
                AddEntryScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    initialCategoryId = entry.arguments?.getString("categoryId")
                )
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    repository = categoryRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    settingsStore = settingsStore,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settingsStore = settingsStore,
                    backupRepository = backupRepository,
                    onOpenExclusionList = { navController.navigate(Routes.EXCLUSION_LIST) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.EXCLUSION_LIST) {
                ExclusionListScreen(
                    installedAppsProvider = installedAppsProvider,
                    excludedAppRepository = excludedAppRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

/**
 * "Таблетка" нижнього навбару з Figma-фрейму Everyday_Designs (node 1930:250): напівпрозорий
 * фон, Home + Статистика як вкладки (popUpTo+launchSingleTop+restoreState — стандартний Compose
 * Navigation патерн, щоб перемикання між ними не нарощувало backstack), "+" по центру — окремий
 * колірний кружок, ОДНОРАЗОВИЙ перехід на Add Entry (не вкладка, тому без popUpTo/selected-стану).
 */
@Composable
private fun TeperaBottomNavBar(currentRoute: String?, navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(TeperaPalette.navPill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavPillTab(
            icon = Icons.Outlined.Home,
            contentDescription = stringResource(R.string.home_screen_title),
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = {
                if (currentRoute != Routes.HOME) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(TeperaPalette.addButtonBackground)
                .clickable { navController.navigate(Routes.ADD_ENTRY) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_entry_button), tint = Color.Black)
        }
        NavPillTab(
            icon = Icons.Outlined.BarChart,
            contentDescription = stringResource(R.string.stats_nav_action),
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = {
                if (currentRoute != Routes.STATS) {
                    navController.navigate(Routes.STATS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}

@Composable
private fun NavPillTab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = Color.Black)
        }
    }
}
