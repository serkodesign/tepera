package com.serkodesign.tepera.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.serkodesign.tepera.debug.SpikeT1Screen
import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BackupRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.GateEventRepository
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.data.repository.PatternRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import com.serkodesign.tepera.ui.addentry.AddEntryScreen
import com.serkodesign.tepera.ui.category.CategoriesScreen
import com.serkodesign.tepera.ui.diary.DiaryScreen
import com.serkodesign.tepera.ui.gates.GatePauseScreen
import com.serkodesign.tepera.ui.gates.GatesScreen
import com.serkodesign.tepera.ui.home.HomeScreen
import com.serkodesign.tepera.ui.onboarding.OnboardingScreen
import com.serkodesign.tepera.ui.onboarding.CategoryOnboardingScreen
import com.serkodesign.tepera.ui.onboarding.OnlineEstimateOnboardingScreen
import com.serkodesign.tepera.ui.onboarding.ValuesOnboardingScreen
import com.serkodesign.tepera.ui.settings.BackupRestoreScreen
import com.serkodesign.tepera.ui.settings.ExclusionListScreen
import com.serkodesign.tepera.ui.settings.LanguageSettingsScreen
import com.serkodesign.tepera.ui.settings.SettingsScreen
import com.serkodesign.tepera.ui.settings.TrackingSettingsScreen
import com.serkodesign.tepera.ui.stats.StatsScreen
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.teperaGradientBackground

private object Routes {
    const val HOME = "home"
    const val ADD_ENTRY = "add_entry"
    const val ADD_ENTRY_WITH_CATEGORY = "add_entry?categoryId={categoryId}"
    const val EDIT_ENTRY = "edit_entry/{entryId}"
    const val CATEGORIES = "categories"
    const val ONBOARDING = "onboarding"
    const val VALUES_ONBOARDING = "values_onboarding"
    const val CATEGORY_ONBOARDING = "category_onboarding"
    const val ONLINE_ESTIMATE_ONBOARDING = "online_estimate_onboarding"
    const val SETTINGS = "settings"
    const val TRACKING_SETTINGS = "tracking_settings"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val EXCLUSION_LIST = "exclusion_list"
    const val BACKUP_RESTORE = "backup_restore"
    const val STATS = "stats"
    const val DIARY = "diary"
    const val SPIKE_T1 = "spike_t1"
    const val GATES = "gates"

    // Три вкладки нижнього навбару, node 2146:320 (Figma, замінив попередній фрейм 1951:4017,
    // де третя вкладка була "pending"-іконкою без екрана) — Home, Diary, Stats, у цьому порядку
    // (Diary — посередині, не праворуч). За прямим запитом користувача заглушку "Незабаром"
    // прибрано, замість неї — повноцінна вкладка "Щоденник" (колишня `HistoryCard` зі Статистики).
    // Кнопки "+" в навбарі нема: додавання часу — per-категорійне (CategoryCard.onAddTime у
    // HomeScreen.kt) через ADD_ENTRY_WITH_CATEGORY. Налаштування відкриваються іконкою-шестернею
    // на Home, не вкладкою навбару.
    val BOTTOM_NAV_ROUTES = setOf(HOME, DIARY, STATS)

    const val GATE_PAUSE = "gate_pause/{packageName}"

    // Екрани, для яких уже є Figma-дизайн (сторінка "Tepera", node 1873:2567) — градієнтний фон
    // малює зовнішній Box у TeperaNavHost для ВСІХ них, не лише для вкладок навбару. **За прямим
    // запитом користувача Онбординг/Додати активність/паузу воріт теж переведено на "скляний"
    // стиль решти застосунку** — раніше вони свідомо лишались на дефолтній Material 3 темі
    // (доки для них не було Figma-фрейму), тепер стилізовані за зразком уже готових екранів
    // (Налаштування/Категорії), без окремого фрейму для кожного.
    val GRADIENT_ROUTES = BOTTOM_NAV_ROUTES + setOf(
        SETTINGS, TRACKING_SETTINGS, LANGUAGE_SETTINGS, CATEGORIES, EXCLUSION_LIST, BACKUP_RESTORE, GATES,
        ADD_ENTRY, ADD_ENTRY_WITH_CATEGORY, EDIT_ENTRY,
        ONBOARDING, VALUES_ONBOARDING, CATEGORY_ONBOARDING, ONLINE_ESTIMATE_ONBOARDING,
        GATE_PAUSE
    )

    fun addEntry(categoryId: String? = null) =
        if (categoryId != null) "add_entry?categoryId=$categoryId" else ADD_ENTRY

    fun editEntry(entryId: String) = "edit_entry/$entryId"

    fun gatePause(packageName: String) = "gate_pause/$packageName"
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
    sleepWindowRepository: SleepWindowRepository,
    userEstimateRepository: UserEstimateRepository,
    unlockRepository: UnlockRepository,
    activeTimerStore: ActiveTimerStore,
    backupRepository: BackupRepository,
    gateRepository: GateRepository,
    gateEventRepository: GateEventRepository,
    cardHistoryRepository: CardHistoryRepository,
    navController: NavHostController = rememberNavController(),
    // FR-4.1/4.4: тап по кнопці категорії на віджеті або по Quick Settings tile відкриває
    // MainActivity з цим "натяком" — обробляється один раз при вході, не при кожній рекомпозиції.
    pendingOpenAddEntry: Boolean = false,
    pendingCategoryId: String? = null,
    // T-5 (tepera-dev-spec.md): тап по закріпленому ярлику воріт (T-4). pendingGateRequestNonce —
    // ключ LaunchedEffect: на відміну від pendingOpenAddEntry (обробляється раз при вході),
    // ворота можуть відкриватись повторно з ТИМ САМИМ packageName (MainActivity.onNewIntent()) —
    // без унікального nonce на кожен тап LaunchedEffect(pendingGateTargetPackage) не перезапустився
    // б, якщо застосунок збігається з попереднім.
    pendingGateTargetPackage: String? = null,
    pendingGateRequestNonce: Long? = null
) {
    LaunchedEffect(Unit) {
        if (pendingOpenAddEntry) {
            navController.navigate(Routes.addEntry(pendingCategoryId))
        }
    }

    LaunchedEffect(pendingGateRequestNonce) {
        if (pendingGateTargetPackage != null) {
            navController.navigate(Routes.gatePause(pendingGateTargetPackage))
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
                    sleepWindowRepository = sleepWindowRepository,
                    userEstimateRepository = userEstimateRepository,
                    unlockRepository = unlockRepository,
                    activeTimerStore = activeTimerStore,
                    cardHistoryRepository = cardHistoryRepository,
                    gateEventRepository = gateEventRepository,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onAddEntryForCategory = { categoryId -> navController.navigate(Routes.addEntry(categoryId)) },
                    onShowOnboarding = { navController.navigate(Routes.ONBOARDING) },
                    onShowValuesOnboarding = { navController.navigate(Routes.VALUES_ONBOARDING) },
                    onShowCategoryOnboarding = { navController.navigate(Routes.CATEGORY_ONBOARDING) },
                    onShowOnlineEstimateOnboarding = { navController.navigate(Routes.ONLINE_ESTIMATE_ONBOARDING) }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    balanceRepository = balanceRepository,
                    patternRepository = patternRepository,
                    settingsStore = settingsStore,
                    sleepWindowRepository = sleepWindowRepository,
                    unlockRepository = unlockRepository,
                    pauseRepository = pauseRepository
                )
            }
            composable(Routes.DIARY) {
                DiaryScreen(
                    activityRepository = activityRepository,
                    categoryRepository = categoryRepository,
                    balanceRepository = balanceRepository,
                    sleepWindowRepository = sleepWindowRepository,
                    unlockRepository = unlockRepository,
                    pauseRepository = pauseRepository,
                    onEditEntry = { entryId -> navController.navigate(Routes.editEntry(entryId)) },
                    onAddEntry = { navController.navigate(Routes.addEntry()) }
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
            composable(
                Routes.EDIT_ENTRY,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                AddEntryScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    editingEntryId = entry.arguments?.getString("entryId")
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
            composable(Routes.CATEGORY_ONBOARDING) {
                CategoryOnboardingScreen(
                    categoryRepository = categoryRepository,
                    settingsStore = settingsStore,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.ONLINE_ESTIMATE_ONBOARDING) {
                OnlineEstimateOnboardingScreen(
                    settingsStore = settingsStore,
                    userEstimateRepository = userEstimateRepository,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenTracking = { navController.navigate(Routes.TRACKING_SETTINGS) },
                    onOpenLanguage = { navController.navigate(Routes.LANGUAGE_SETTINGS) },
                    onOpenExclusionList = { navController.navigate(Routes.EXCLUSION_LIST) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenBackupRestore = { navController.navigate(Routes.BACKUP_RESTORE) },
                    onOpenGates = { navController.navigate(Routes.GATES) },
                    onOpenSpikeT1 = { navController.navigate(Routes.SPIKE_T1) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.TRACKING_SETTINGS) {
                TrackingSettingsScreen(
                    settingsStore = settingsStore,
                    sleepWindowRepository = sleepWindowRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.LANGUAGE_SETTINGS) {
                LanguageSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SPIKE_T1) {
                SpikeT1Screen(onBack = { navController.popBackStack() })
            }
            composable(Routes.GATES) {
                GatesScreen(
                    gateRepository = gateRepository,
                    installedAppsProvider = installedAppsProvider,
                    settingsStore = settingsStore,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Routes.GATE_PAUSE,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { entry ->
                GatePauseScreen(
                    gateRepository = gateRepository,
                    gateEventRepository = gateEventRepository,
                    packageName = entry.arguments?.getString("packageName").orEmpty(),
                    // popBackStack(HOME, inclusive = false) замість одного кроку назад: якщо
                    // ворота відкрились, поки застосунок уже стояв на іншому екрані (напр.
                    // GatesScreen), звичайний одиничний pop повернув би саме туди — а разом із
                    // moveTaskToBack() у GatePauseScreen це означало б, що НАСТУПНЕ відкриття
                    // Tepera з лаунчера показує проміжний екран замість Home.
                    onDone = { navController.popBackStack(Routes.HOME, inclusive = false) }
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
 * "Таблетка" нижнього навбару — точна відповідність Figma-фрейму "Everyday_Designs", node
 * 2146:320 (get_design_context + get_variable_defs): біла картка (Surface/surface-card,
 * замінила попередню суцільну темно-зелену з node 1951:4017 — `TeperaPalette.navPillDark`
 * лишений у палітрі як історія рішення), 3 РІВНОВЕЛИКІ вкладки Home/Diary/Stats (у цьому
 * порядку — Diary посередині, не праворуч). Вибрана вкладка — м'ятна підсвітка (Brand/200) з
 * текстом і темно-зеленою іконкою (Brand/800), невибрані — лише сіра іконка (Text/text-
 * secondary), без підпису. Іконки — `TeperaIcons` (SVG-точні вектори з того самого фрейму, не
 * найближчі глифи material-icons-extended). Кнопки "+"/"Незабаром" по центру більше нема —
 * заглушку прибрано (за запитом користувача), додавання часу лишається per-категорійним
 * (HomeScreen.CategoryCard).
 */
@Composable
private fun TeperaBottomNavBar(currentRoute: String?, navController: NavHostController) {
    // enableEdgeToEdge() (MainActivity) малює контент ПІД системними барами — без урахування
    // WindowInsets.navigationBars "таблетка" на фіксованому bottom-відступі ховалась під
    // системним навбаром на пристроях з високим 3-кнопковим навбаром (підтверджено на Huawei
    // P9, EMUI) — на Samsung S23 із жестовою навігацією (тонша смуга) цього не було помітно.
    // 8.dp зверху системного інсету — той самий подих, що раніше давав фіксований 24.dp.
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + navigationBarInset)
            .height(62.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(TeperaPalette.navPillCard)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavPillTab(
            icon = TeperaIcons.Home,
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
            icon = TeperaIcons.Ballot,
            label = stringResource(R.string.diary_nav_action),
            selected = currentRoute == Routes.DIARY,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = {
                if (currentRoute != Routes.DIARY) {
                    navController.navigate(Routes.DIARY) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavPillTab(
            icon = TeperaIcons.Leaderboard,
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
    }
}

@Composable
private fun NavPillTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) TeperaPalette.navPillSelectedContent else TeperaPalette.navPillUnselectedIcon
    val shape = RoundedCornerShape(40.dp)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            // Тінь вибраної вкладки (drop-shadow з фрейму) прибрана за прямим запитом користувача.
            .clip(shape)
            .then(if (selected) Modifier.background(TeperaPalette.navPillSelected) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            // Відстань іконка-підпис зменшена з 4.dp до 2.dp за прямим запитом користувача.
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = if (selected) null else label, tint = contentColor)
            if (selected) {
                // maxLines/softWrap: "Щоденник" (9 символів) ледь не влазить у третину ширини
                // навбару поруч з іконкою й переносився на 2 рядки, ламаючи висоту "таблетки"
                // (перевірено живцем на Huawei P9) — коротші "Сьогодні"/"Огляд" цього не показали,
                // тому в макеті це не було видно. Один рядок, з "…" як крайній запобіжник.
                Text(
                    label,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = TeperaPalette.headlineFont,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
