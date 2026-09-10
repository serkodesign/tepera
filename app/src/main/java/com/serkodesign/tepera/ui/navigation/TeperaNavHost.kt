package com.serkodesign.tepera.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.serkodesign.tepera.data.repository.PatternRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.ui.addentry.AddEntryScreen
import com.serkodesign.tepera.ui.category.CategoriesScreen
import com.serkodesign.tepera.ui.home.HomeScreen
import com.serkodesign.tepera.ui.onboarding.OnboardingScreen
import com.serkodesign.tepera.ui.onboarding.ValuesOnboardingScreen
import com.serkodesign.tepera.ui.settings.BackupRestoreScreen
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
    const val VALUES_ONBOARDING = "values_onboarding"
    const val SETTINGS = "settings"
    const val EXCLUSION_LIST = "exclusion_list"
    const val BACKUP_RESTORE = "backup_restore"
    const val STATS = "stats"

    // Три вкладки нижнього навбару (оновлений Figma-фрейм, node 1951:4017): Home, Статистика,
    // і третя ("pending"-іконка) — за запитом користувача додана як вкладка, але поки що
    // НЕактивна (немає екрана в фреймі, який вона мала б відкривати). Кнопки "+" в навбарі
    // більше нема: додавання часу тепер per-категорійне (see CategoryCard.onAddTime у
    // HomeScreen.kt) через ADD_ENTRY_WITH_CATEGORY, а не через загальний вибір категорії.
    // Налаштування відкриваються іконкою-шестернею на Home, не вкладкою навбару.
    val BOTTOM_NAV_ROUTES = setOf(HOME, STATS)

    // Екрани, для яких уже є Figma-дизайн (сторінка "Tepera", node 1873:2567) — градієнтний фон
    // малює зовнішній Box у TeperaNavHost для ВСІХ них, не лише для вкладок навбару. Онбординг і
    // Add Entry свідомо лишаються поза цим списком — для них ще нема окремого фрейму.
    val GRADIENT_ROUTES = BOTTOM_NAV_ROUTES + setOf(SETTINGS, CATEGORIES, EXCLUSION_LIST, BACKUP_RESTORE)

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
    pauseRepository: PauseRepository,
    patternRepository: PatternRepository,
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
    val useGradientBackground = currentRoute in Routes.GRADIENT_ROUTES
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
            // Навмисно БЕЗ нижнього відступу scaffoldPadding: інакше екрани з навбаром-"таблеткою"
            // (Home, Статистика) отримують подвійний нижній inset (тут + власний Scaffold
            // усередині HomeScreen) і картки категорій обрізаються, не влазячи в стиснуту область.
            // Замість цього контент тепер сягає самого низу екрана, а те, що не влазить, за
            // запитом користувача просто заходить під напівпрозору "таблетку" навбару (вона
            // малюється поверх контенту, бо bottomBar розміщується останнім у Scaffold).
            val layoutDirection = LocalLayoutDirection.current
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(
                    top = scaffoldPadding.calculateTopPadding(),
                    start = scaffoldPadding.calculateStartPadding(layoutDirection),
                    end = scaffoldPadding.calculateEndPadding(layoutDirection)
                )
            ) {
            composable(Routes.HOME) {
                HomeScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    balanceRepository = balanceRepository,
                    pauseRepository = pauseRepository,
                    patternRepository = patternRepository,
                    settingsStore = settingsStore,
                    activeTimerStore = activeTimerStore,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onAddEntryForCategory = { categoryId -> navController.navigate(Routes.addEntry(categoryId)) },
                    onShowOnboarding = { navController.navigate(Routes.ONBOARDING) },
                    onShowValuesOnboarding = { navController.navigate(Routes.VALUES_ONBOARDING) }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    balanceRepository = balanceRepository,
                    patternRepository = patternRepository,
                    settingsStore = settingsStore
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
            composable(Routes.VALUES_ONBOARDING) {
                ValuesOnboardingScreen(
                    categoryRepository = categoryRepository,
                    settingsStore = settingsStore,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settingsStore = settingsStore,
                    onOpenExclusionList = { navController.navigate(Routes.EXCLUSION_LIST) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenBackupRestore = { navController.navigate(Routes.BACKUP_RESTORE) },
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
            composable(Routes.BACKUP_RESTORE) {
                BackupRestoreScreen(
                    backupRepository = backupRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

/**
 * "Таблетка" нижнього навбару з оновленого Figma-фрейму (Home screen, node 1951:4017): суцільна
 * темно-зелена напівпрозора підложка (не біла, як раніше), 3 РІВНОВЕЛИКІ вкладки — вибрана
 * показує іконку+підпис на світлішій підсвітці, невибрані лишень іконку. Кнопки "+" по центру
 * більше нема (за запитом користувача — додавання часу тепер per-категорійне, див.
 * HomeScreen.CategoryCard). Третя вкладка ("pending", кружок із трьома крапками) поки що НЕ
 * веде нікуди — у фреймі немає екрана для неї; додана як вкладка, але неактивна (за запитом).
 */
@Composable
private fun TeperaBottomNavBar(currentRoute: String?, navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.navPillDark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavPillTab(
            icon = Icons.Filled.Home,
            label = stringResource(R.string.home_screen_title),
            selected = currentRoute == Routes.HOME,
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
        NavPillTab(
            icon = Icons.Filled.BarChart,
            label = stringResource(R.string.stats_nav_action),
            selected = currentRoute == Routes.STATS,
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
        NavPillTab(
            icon = Icons.Filled.Pending,
            label = stringResource(R.string.nav_more_placeholder),
            selected = false,
            enabled = false,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = { }
        )
    }
}

@Composable
private fun NavPillTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentColor = Color.White.copy(alpha = if (enabled) 1f else 0.4f)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(40.dp))
            .then(if (selected) Modifier.background(TeperaPalette.navPillSelectedHighlight) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = if (selected) null else label, tint = contentColor)
            if (selected) {
                Text(label, color = contentColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
