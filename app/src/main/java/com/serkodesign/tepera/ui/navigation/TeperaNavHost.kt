package com.serkodesign.tepera.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavBackStackEntry
import com.serkodesign.tepera.ui.theme.TeperaMotion
import com.serkodesign.tepera.ui.theme.TeperaSpecs
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.R
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
import com.serkodesign.tepera.util.SavedEntryUndo
import com.serkodesign.tepera.util.ShortcutScreen
import com.serkodesign.tepera.ui.addentry.AddEntryScreen
import com.serkodesign.tepera.ui.category.CategoriesScreen
import com.serkodesign.tepera.ui.category.CategoryHistoryScreen
import com.serkodesign.tepera.ui.diary.DiaryScreen
import com.serkodesign.tepera.ui.knowledge.KnowledgeBaseScreen
import com.serkodesign.tepera.ui.gates.GatePauseScreen
import com.serkodesign.tepera.ui.gates.GatesScreen
import com.serkodesign.tepera.ui.gates.GateScheduleScreen
import com.serkodesign.tepera.ui.home.HomeScreen
import com.serkodesign.tepera.ui.home.MiniPlayer
import com.serkodesign.tepera.data.toggleCategoryTimer
import com.serkodesign.tepera.ui.onboarding.OnboardingScreen
import com.serkodesign.tepera.ui.onboarding.CategoryOnboardingScreen
import com.serkodesign.tepera.ui.onboarding.OnlineEstimateOnboardingScreen
import com.serkodesign.tepera.ui.onboarding.TargetOnboardingScreen
import com.serkodesign.tepera.ui.onboarding.WelcomeOnboardingScreen
import com.serkodesign.tepera.ui.onboarding.GatesOnboardingScreen
import androidx.compose.ui.platform.LocalContext
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.ui.settings.AboutScreen
import com.serkodesign.tepera.ui.settings.ProInterestScreen
import com.serkodesign.tepera.ui.settings.BackupRestoreScreen
import com.serkodesign.tepera.ui.settings.ExclusionListScreen
import com.serkodesign.tepera.debug.RiskSpikeScreen
import com.serkodesign.tepera.ui.risk.FeedPickerScreen
import com.serkodesign.tepera.ui.risk.NudgeInfoScreen
import com.serkodesign.tepera.ui.risk.TesterStatsScreen
import com.serkodesign.tepera.ui.risk.RiskHomeScreen
import com.serkodesign.tepera.ui.risk.PlanEditorScreen
import com.serkodesign.tepera.ui.risk.WindowWhyScreen
import com.serkodesign.tepera.ui.risk.V3AccessScreen
import com.serkodesign.tepera.ui.risk.V3WelcomeScreen
import com.serkodesign.tepera.ui.settings.SettingsScreen
import com.serkodesign.tepera.ui.settings.TrackingSettingsScreen
import com.serkodesign.tepera.ui.stats.StatsScreen
import com.serkodesign.tepera.ui.settings.WidgetSettingsScreen
import com.serkodesign.tepera.ui.splash.SplashScreen
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.LocalTeperaColors
import com.serkodesign.tepera.ui.theme.SystemBarsAppearance
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.teperaGradientBackground
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ADD_ENTRY = "add_entry"
    const val ADD_ENTRY_WITH_CATEGORY = "add_entry?categoryId={categoryId}"
    const val EDIT_ENTRY = "edit_entry/{entryId}"
    const val CATEGORIES = "categories"
    const val ONBOARDING = "onboarding"
    const val CATEGORY_ONBOARDING = "category_onboarding"
    const val ONLINE_ESTIMATE_ONBOARDING = "online_estimate_onboarding"
    const val TARGET_ONBOARDING = "target_onboarding"
    const val WELCOME_ONBOARDING = "welcome_onboarding"
    const val GATES_ONBOARDING = "gates_onboarding"
    const val SETTINGS = "settings"
    const val TRACKING_SETTINGS = "tracking_settings"
    const val EXCLUSION_LIST = "exclusion_list"
    const val BACKUP_RESTORE = "backup_restore"
    const val ABOUT = "about"
    const val RISK_SPIKE = "risk_spike"
    const val FEED_PICKER = "feed_picker"
    const val NUDGE_INFO = "nudge_info"
    const val TESTER_STATS = "tester_stats"
    const val V3_WELCOME = "v3_welcome"
    const val V3_ACCESS = "v3_access"
    const val V3_FEEDS = "v3_feeds"
    const val V3_PLAN = "v3_plan/{windowId}"
    const val V3_WHY = "v3_why/{windowId}"
    const val PRO_INTEREST = "pro_interest"
    const val GATE_SCHEDULE = "gate_schedule"
    const val STATS = "stats"
    const val DIARY = "diary"
    const val GATES = "gates"
    const val WIDGET_SETTINGS = "widget_settings"
    const val KNOWLEDGE_BASE = "knowledge_base"
    const val KNOWLEDGE_SCROLLING = "knowledge_scrolling"
    const val CATEGORY_HISTORY = "category_history/{categoryId}"

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
        SETTINGS, TRACKING_SETTINGS, CATEGORIES, EXCLUSION_LIST, BACKUP_RESTORE, ABOUT, RISK_SPIKE, FEED_PICKER, NUDGE_INFO, TESTER_STATS, V3_WELCOME, V3_ACCESS, V3_FEEDS, V3_PLAN, V3_WHY, PRO_INTEREST, GATES, GATE_SCHEDULE, WIDGET_SETTINGS,
        ADD_ENTRY, ADD_ENTRY_WITH_CATEGORY, EDIT_ENTRY,
        ONBOARDING, CATEGORY_ONBOARDING, ONLINE_ESTIMATE_ONBOARDING, TARGET_ONBOARDING,
        WELCOME_ONBOARDING, GATES_ONBOARDING, GATE_PAUSE, KNOWLEDGE_BASE, KNOWLEDGE_SCROLLING, CATEGORY_HISTORY
    )

    fun addEntry(categoryId: String? = null) =
        if (categoryId != null) "add_entry?categoryId=$categoryId" else ADD_ENTRY

    fun editEntry(entryId: String) = "edit_entry/$entryId"

    fun gatePause(packageName: String) = "gate_pause/$packageName"

    fun categoryHistory(categoryId: String) = "category_history/$categoryId"
    fun v3Plan(windowId: String) = "v3_plan/$windowId"
    fun v3Why(windowId: String) = "v3_why/$windowId"
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
    pendingGateRequestNonce: Long? = null,
    pendingWeeklySummaryNonce: Long? = null,
    // Ярлик "Щоденник"/"Статистика" (довгий тап по іконці). Nonce — як у воріт: повторний тап по
    // тому самому ярлику має знову перейти на екран.
    pendingShortcutScreen: ShortcutScreen? = null,
    pendingShortcutNonce: Long? = null
) {
    // Заставка — лише при першому запуску (за прямим запитом користувача): системний
    // Android-спалах (MainActivity, windowSplashScreenBackground/AnimatedIcon) уже показується
    // при КОЖНОМУ холодному старті, тож власна Compose-заставка поверх нього на звичайному
    // перезапуску (застосунок вивантажено з пам'яті) дублювала б той самий момент двома різними
    // екранами поспіль. `settingsStore.splashScreenSeen` (DataStore) вирішує старт NavHost — поки
    // перше значення не прийшло (один кадр), NavHost узагалі не композиться: той самий підхід
    // "мовчати, доки не знаємо", що вже є для `hasUsageAccess == null` на Home.
    val splashScreenSeen by settingsStore.splashScreenSeen.collectAsState(initial = null)
    if (splashScreenSeen == null) return
    val startDestination = if (splashScreenSeen == true) Routes.HOME else Routes.SPLASH
    val splashScope = rememberCoroutineScope()

    // Заставка (Routes.SPLASH) — стартова точка NavHost, тож ці "натяки" (тап по кнопці
    // категорії на віджеті/Quick Settings, тап по ярлику воріт) можуть спрацювати ДО того, як
    // Home взагалі побував у стеку. skipSplash() вставляє Home під цільовий екран замість неї —
    // інакше системна "назад" повернула б на ще не показану заставку (і повторно запустила б її
    // таймер), а popBackStack(Routes.HOME, ...) нижче (ворота/тижневе сповіщення) не знайшов би
    // HOME у стеку взагалі.
    fun skipSplash() {
        if (navController.currentDestination?.route == Routes.SPLASH) {
            navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
        }
    }

    LaunchedEffect(Unit) {
        if (pendingOpenAddEntry) {
            skipSplash()
            navController.navigate(Routes.addEntry(pendingCategoryId))
        }
    }

    LaunchedEffect(pendingGateRequestNonce) {
        if (pendingGateTargetPackage != null) {
            skipSplash()
            navController.navigate(Routes.gatePause(pendingGateTargetPackage))
        }
    }

    // Ворота, з яких пішли без вибору («Додому», інший застосунок, блокування екрана), не лишаються в стеку: інакше
    // наступне відкриття Tepera з лаунчера показало б стару паузу замість Home. Подію рішення не пишемо — вибору не було.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (navController.currentDestination?.route == Routes.GATE_PAUSE) {
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    // CC-8: тап по тижневому сповіщенню — повернутися на Home, де лежить картка «Цей тиждень».
    LaunchedEffect(pendingWeeklySummaryNonce) {
        if (pendingWeeklySummaryNonce != null) {
            skipSplash()
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    // Ярлик "Щоденник"/"Статистика": перехід як тап по вкладці навбару (та сама схема popUpTo/restoreState).
    LaunchedEffect(pendingShortcutNonce) {
        val screen = pendingShortcutScreen ?: return@LaunchedEffect
        skipSplash()
        navController.navigate(
            when (screen) {
                ShortcutScreen.DIARY -> Routes.DIARY
                ShortcutScreen.STATS -> Routes.STATS
            }
        ) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Онбординг — єдиний ланцюжок: кожен крок веде просто до наступного (з заміною себе в стеку),
    // Home між кроками не показується. Порядок і умови (доступ до статистики визначає, чи буде
    // крок дозволу або орієнтиру) — у [nextOnboardingRoute]; коли кроків не лишилось — назад на Home.
    val onboardingScope = rememberCoroutineScope()
    fun advanceOnboarding(from: String) {
        onboardingScope.launch {
            val next = nextOnboardingRoute(settingsStore, balanceRepository)
            if (next == null) {
                navController.popBackStack(Routes.HOME, inclusive = false)
            } else {
                navController.navigate(next) { popUpTo(from) { inclusive = true } }
            }
        }
    }

    // Тихий вихід з усього ланцюжка онбордингу одним тапом (за прямим запитом користувача) —
    // "Пропустити все" у правому верхньому куті кожного кроку. Позначає решту кроків побаченими
    // одним записом (`skipAllOnboarding()`) і повертає на Home тим самим шляхом, що звичайне
    // завершення ланцюжка вище — Home уже лежить у стеку під поточним кроком.
    fun skipAllOnboarding() {
        onboardingScope.launch {
            settingsStore.skipAllOnboarding()
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Повторний тап по вже вибраній вкладці навбару (за прямим запитом користувача) прокручує
    // той екран наверх замість нічого не робити — кожен лічильник передається у відповідний
    // екран (HomeScreen/DiaryScreen/StatsScreen) як scrollToTopSignal, і зміна значення (не саме
    // значення) запускає LaunchedEffect(scrollToTopSignal) там.
    var homeScrollToTopSignal by remember { mutableStateOf(0) }
    var diaryScrollToTopSignal by remember { mutableStateOf(0) }
    var statsScrollToTopSignal by remember { mutableStateOf(0) }

    // Градієнт застосовується ТУТ, на самому зовнішньому Box (а не всередині HomeScreen/
    // StatsScreen) — інакше він потрапляє під contentPadding зовнішнього Scaffold і не сягає
    // країв екрана (status bar/навбар лишаються білою смугою поверх, підтверджено на
    // Samsung S23). Умовний, не глобальний: Налаштування/Категорії/Додати активність — досі
    // дефолтна Material 3 тема, для них Figma-дизайну ще нема.
    // Екран дозволів (ONBOARDING) — раніше єдиний темний виняток застосунку (#12171F +
    // PermissionsBackground()); за наданим Figma-редизайном (node 292:1587) перестилізований на той
    // самий світлий градієнт, що решта GRADIENT_ROUTES — окремого прапорця більше не потрібно.
    val useGradientBackground = currentRoute in Routes.GRADIENT_ROUTES

    // ui-redesign (TeperaColors.kt): набір кольорів задає TeperaTheme за системним світлим/темним режимом; тут лише
    // іконки системних барів під нього.
    SystemBarsAppearance(lightBars = LocalTeperaColors.current.lightSystemBars)
    // Матове скло навбару (Haze): джерело розмиття — фон і контент екрана (окремий Box нижче), навбар
    // малюється ПОВЕРХ нього, а не всередині Scaffold, інакше він потрапив би у власне джерело.
    val hazeState = remember { HazeState() }
    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier.hazeSource(hazeState).then(
            when {
                // Заставка малює власний повний фон (SplashScreen.kt) — тут лишень fillMaxSize,
                // без подвійного малювання позаду неї.
                currentRoute == Routes.SPLASH -> Modifier.fillMaxSize()
                useGradientBackground -> Modifier.teperaGradientBackground()
                else -> Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            }
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent
        ) { scaffoldPadding ->
            // Навмисно БЕЗ нижнього відступу scaffoldPadding: інакше екрани з навбаром-"таблеткою"
            // (Home, Статистика) отримують подвійний нижній inset (тут + власний Scaffold
            // усередині HomeScreen) і картки категорій обрізаються, не влазячи в стиснуту область.
            // Замість цього контент тепер сягає самого низу екрана, а те, що не влазить, за
            // запитом користувача просто заходить під напівпрозору "таблетку" навбару (вона
            // малюється поверх контенту, бо bottomBar розміщується останнім у Scaffold).
            val layoutDirection = LocalLayoutDirection.current
            // Переходи між екранами за патернами M3: між вкладками навбару — "fade through"
            // (зникнення + поява зі збільшенням 92% -> 100%), для решти — "shared axis X" (зсув на
            // 30dp разом із fade вперед/назад). Тривалість/криві — з [TeperaMotion].
            val slidePx = with(LocalDensity.current) { 30.dp.roundToPx() }
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(
                    top = scaffoldPadding.calculateTopPadding(),
                    start = scaffoldPadding.calculateStartPadding(layoutDirection),
                    end = scaffoldPadding.calculateEndPadding(layoutDirection)
                ),
                enterTransition = {
                    if (isTopLevelSwitch()) {
                        fadeIn(TeperaSpecs.enterFade()) +
                            scaleIn(tween(TeperaMotion.MEDIUM2, easing = TeperaMotion.Emphasized), initialScale = 0.92f)
                    } else {
                        slideInHorizontally(TeperaSpecs.spatial()) { slidePx } +
                            fadeIn(TeperaSpecs.enterFade())
                    }
                },
                exitTransition = {
                    if (isTopLevelSwitch()) {
                        fadeOut(TeperaSpecs.exitFade())
                    } else {
                        slideOutHorizontally(TeperaSpecs.spatial()) { -slidePx } + fadeOut(TeperaSpecs.exitFade())
                    }
                },
                popEnterTransition = {
                    if (isTopLevelSwitch()) {
                        fadeIn(TeperaSpecs.enterFade()) +
                            scaleIn(tween(TeperaMotion.MEDIUM2, easing = TeperaMotion.Emphasized), initialScale = 0.92f)
                    } else {
                        slideInHorizontally(TeperaSpecs.spatial()) { -slidePx } +
                            fadeIn(TeperaSpecs.enterFade())
                    }
                },
                popExitTransition = {
                    if (isTopLevelSwitch()) {
                        fadeOut(TeperaSpecs.exitFade())
                    } else {
                        slideOutHorizontally(TeperaSpecs.spatial()) { slidePx } + fadeOut(TeperaSpecs.exitFade())
                    }
                }
            ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        splashScope.launch { settingsStore.setSplashScreenSeen() }
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                // v3.0 MVP (beta): режим «Години ризику» замінює Home; null = ще читається — порожній кадр, щоб не мигав старий Home.
                val v3Mode by settingsStore.v3ModeEnabled.collectAsState(initial = null)
                if (BuildConfig.V3_MVP && v3Mode == null) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize())
                } else if (BuildConfig.V3_MVP && v3Mode == true) {
                    val v3Seen by settingsStore.v3OnboardingSeen.collectAsState(initial = null)
                    LaunchedEffect(v3Seen) { if (v3Seen == false) navController.navigate(Routes.V3_WELCOME) }
                    RiskHomeScreen(
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenKnowledgeBase = { navController.navigate(Routes.KNOWLEDGE_BASE) },
                        onOpenFeedPicker = { navController.navigate(Routes.FEED_PICKER) },
                        onOpenAccess = { navController.navigate(Routes.V3_ACCESS) },
                        onOpenPlan = { windowId -> navController.navigate(Routes.v3Plan(windowId)) },
                        onOpenWhy = { windowId -> navController.navigate(Routes.v3Why(windowId)) }
                    )
                } else
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
                    onOpenKnowledgeBase = { navController.navigate(Routes.KNOWLEDGE_BASE) },
                    // "Відкрити в статистиці" у шторці підсумків — той самий перехід, що вкладка "Статистика" в навбарі.
                    onOpenStats = {
                        navController.navigate(Routes.STATS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddEntryForCategory = { categoryId -> navController.navigate(Routes.addEntry(categoryId)) },
                    onOpenCategoryHistory = { categoryId -> navController.navigate(Routes.categoryHistory(categoryId)) },
                    onShowOnboardingStep = {
                        onboardingScope.launch {
                            nextOnboardingRoute(settingsStore, balanceRepository)?.let { navController.navigate(it) }
                        }
                    },
                    scrollToTopSignal = homeScrollToTopSignal,
                    weeklySummaryNonce = pendingWeeklySummaryNonce
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
                    pauseRepository = pauseRepository,
                    scrollToTopSignal = statsScrollToTopSignal
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
                    onAddEntry = { navController.navigate(Routes.addEntry()) },
                    scrollToTopSignal = diaryScrollToTopSignal
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
            composable(Routes.WIDGET_SETTINGS) {
                WidgetSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    repository = categoryRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Routes.CATEGORY_HISTORY,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { entry ->
                CategoryHistoryScreen(
                    categoryRepository = categoryRepository,
                    activityRepository = activityRepository,
                    categoryId = entry.arguments?.getString("categoryId").orEmpty(),
                    onEditEntry = { entryId -> navController.navigate(Routes.editEntry(entryId)) },
                    onAddEntry = { navController.navigate(Routes.addEntry(entry.arguments?.getString("categoryId").orEmpty())) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.KNOWLEDGE_BASE) {
                KnowledgeBaseScreen(
                    onOpenScrollingNotes = { navController.navigate(Routes.KNOWLEDGE_SCROLLING) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.KNOWLEDGE_SCROLLING) {
                com.serkodesign.tepera.ui.knowledge.ScrollingNotesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    settingsStore = settingsStore,
                    balanceRepository = balanceRepository,
                    onDone = { advanceOnboarding(Routes.ONBOARDING) },
                    onSkipAll = ::skipAllOnboarding
                )
            }
            composable(Routes.CATEGORY_ONBOARDING) {
                CategoryOnboardingScreen(
                    categoryRepository = categoryRepository,
                    settingsStore = settingsStore,
                    onDone = { advanceOnboarding(Routes.CATEGORY_ONBOARDING) },
                    onSkipAll = ::skipAllOnboarding
                )
            }
            composable(Routes.ONLINE_ESTIMATE_ONBOARDING) {
                OnlineEstimateOnboardingScreen(
                    settingsStore = settingsStore,
                    userEstimateRepository = userEstimateRepository,
                    onDone = { advanceOnboarding(Routes.ONLINE_ESTIMATE_ONBOARDING) },
                    onSkipAll = ::skipAllOnboarding
                )
            }
            composable(Routes.TARGET_ONBOARDING) {
                TargetOnboardingScreen(
                    settingsStore = settingsStore,
                    onDone = { advanceOnboarding(Routes.TARGET_ONBOARDING) },
                    onSkipAll = ::skipAllOnboarding
                )
            }
            composable(Routes.WELCOME_ONBOARDING) {
                WelcomeOnboardingScreen(
                    settingsStore = settingsStore,
                    onDone = { advanceOnboarding(Routes.WELCOME_ONBOARDING) },
                    onSkipAll = ::skipAllOnboarding
                )
            }
            composable(Routes.GATES_ONBOARDING) {
                GatesOnboardingScreen(
                    settingsStore = settingsStore,
                    onDone = { advanceOnboarding(Routes.GATES_ONBOARDING) },
                    onSkipAll = ::skipAllOnboarding
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenTracking = { navController.navigate(Routes.TRACKING_SETTINGS) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenDataPrivacy = { navController.navigate(Routes.BACKUP_RESTORE) },
                    onOpenGates = { navController.navigate(Routes.GATES) },
                    onOpenWidgetSettings = { navController.navigate(Routes.WIDGET_SETTINGS) },
                    onOpenKnowledgeBase = { navController.navigate(Routes.KNOWLEDGE_BASE) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenProInterest = { navController.navigate(Routes.PRO_INTEREST) },
                    onOpenFeedPicker = { navController.navigate(Routes.FEED_PICKER) },
                    onOpenNudgeInfo = { navController.navigate(Routes.NUDGE_INFO) },
                    settingsStore = settingsStore,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.TRACKING_SETTINGS) {
                TrackingSettingsScreen(
                    settingsStore = settingsStore,
                    sleepWindowRepository = sleepWindowRepository,
                    balanceRepository = balanceRepository,
                    onOpenExclusionList = { navController.navigate(Routes.EXCLUSION_LIST) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.GATES) {
                GatesScreen(
                    gateRepository = gateRepository,
                    installedAppsProvider = installedAppsProvider,
                    onOpenSchedule = { navController.navigate(Routes.GATE_SCHEDULE) },
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
            composable(Routes.GATE_SCHEDULE) {
                GateScheduleScreen(
                    gateRepository = gateRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PRO_INTEREST) {
                ProInterestScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    onOpenTesterStats = { navController.navigate(Routes.TESTER_STATS) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.V3_WELCOME) {
                // Системне «назад» на першому екрані онбордингу виходить із застосунку (інакше Home одразу повертає сюди).
                val activity = androidx.compose.ui.platform.LocalContext.current.let { ctx ->
                    generateSequence(ctx) { (it as? android.content.ContextWrapper)?.baseContext }.filterIsInstance<android.app.Activity>().firstOrNull()
                }
                androidx.activity.compose.BackHandler { activity?.finish() }
                V3WelcomeScreen(onStart = { navController.navigate(Routes.V3_ACCESS) })
            }
            composable(Routes.V3_ACCESS) {
                V3AccessScreen(
                    onGranted = {
                        navController.navigate(Routes.V3_FEEDS) { popUpTo(Routes.V3_WELCOME) { inclusive = true } }
                    }
                )
            }
            composable(Routes.V3_FEEDS) {
                FeedPickerScreen(
                    onBack = { navController.popBackStack() },
                    onDone = {
                        onboardingScope.launch {
                            settingsStore.setV3OnboardingSeen(true)
                            navController.popBackStack()
                        }
                    }
                )
            }
            composable(
                Routes.V3_PLAN,
                arguments = listOf(navArgument("windowId") { type = NavType.StringType })
            ) { entry ->
                PlanEditorScreen(
                    windowId = entry.arguments?.getString("windowId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Routes.V3_WHY,
                arguments = listOf(navArgument("windowId") { type = NavType.StringType })
            ) { entry ->
                WindowWhyScreen(
                    windowId = entry.arguments?.getString("windowId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.FEED_PICKER) { FeedPickerScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.NUDGE_INFO) { NudgeInfoScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.TESTER_STATS) {
                TesterStatsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTools = { navController.navigate(Routes.RISK_SPIKE) },
                    onRestartOnboarding = {
                        onboardingScope.launch {
                            settingsStore.setV3OnboardingSeen(false)
                            settingsStore.setV3ModeEnabled(true)
                            navController.navigate(Routes.V3_WELCOME)
                        }
                    }
                )
            }
            composable(Routes.RISK_SPIKE) {
                RiskSpikeScreen(onBack = { navController.popBackStack() })
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
    // Снекбар "Записано · Скасувати" після збереження нового запису — вище за екрани, бо форма вже закрита.
    val undoPending by SavedEntryUndo.pending.collectAsState()
    undoPending?.let { pending ->
        UndoSavedSnackbar(
            pending = pending,
            activityRepository = activityRepository,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    // Міні-плеєр сесії (HANDOFF, розділ 7): над навбаром на вкладках, крім Home (там — активний рядок списку).
    if (currentRoute in Routes.BOTTOM_NAV_ROUTES && currentRoute != Routes.HOME) {
        val activeTimers by activeTimerStore.activeTimers.collectAsState(initial = emptyMap())
        val allCategories by categoryRepository.observeAllCategories().collectAsState(initial = emptyList())
        // Одночасно йде лише один таймер; якщо їх кілька (застарілий стан) — показуємо найновіший.
        val runningCategory = activeTimers.entries.maxByOrNull { it.value }
            ?.let { running -> allCategories.firstOrNull { it.id == running.key } }
        if (runningCategory != null) {
            val miniPlayerScope = rememberCoroutineScope()
            val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            MiniPlayer(
                category = runningCategory,
                onOpen = { navController.navigate(Routes.categoryHistory(runningCategory.id)) },
                onStop = {
                    miniPlayerScope.launch { toggleCategoryTimer(activeTimerStore, activityRepository, runningCategory.id) }
                },
                hazeState = hazeState,
                // Над таблеткою навбару: 62dp висота + 16dp нижній відступ + системний інсет + 8dp проміжок.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 8.dp, end = 8.dp, bottom = 62.dp + 16.dp + navigationBarInset + 8.dp)
            )
        }
    }
    if (currentRoute in Routes.BOTTOM_NAV_ROUTES) {
        TeperaBottomNavBar(
            currentRoute = currentRoute,
            navController = navController,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter),
            onReselected = { route ->
                when (route) {
                    Routes.HOME -> homeScrollToTopSignal++
                    Routes.DIARY -> diaryScrollToTopSignal++
                    Routes.STATS -> statsScrollToTopSignal++
                }
            }
        )
    }
    }
}

/**
 * Наступний непройдений крок онбордингу або null, коли всі пройдені: Welcome → категорії → оцінка Online →
 * орієнтир → ворота → дозвіл (останній, лише без доступу). Пропозиція віджета — тепер картка Home, не крок.
 */
private suspend fun nextOnboardingRoute(settingsStore: SettingsStore, balanceRepository: BalanceRepository): String? {
    if (!settingsStore.welcomeOnboardingSeen.first()) return Routes.WELCOME_ONBOARDING
    if (!settingsStore.categoryOnboardingSeen.first()) return Routes.CATEGORY_ONBOARDING
    if (!settingsStore.onlineEstimateOnboardingSeen.first()) return Routes.ONLINE_ESTIMATE_ONBOARDING
    if (!settingsStore.targetOnboardingSeen.first()) return Routes.TARGET_ONBOARDING
    if (!settingsStore.gatesOnboardingSeen.first()) return Routes.GATES_ONBOARDING
    // Дозвіл — останній крок, і лише якщо доступу ще нема.
    if (!balanceRepository.hasUsageAccess() && !settingsStore.onboardingUsageAccessSeen.first()) return Routes.ONBOARDING
    return null
}

/**
 * "Таблетка" нижнього навбару — Figma "App concept", node 395:1011: скляний контейнер
 * (Surface/surface-card-transparent, білий 30%) заввишки 62 з радіусом 32 і відступом 6, три рівні вкладки
 * Home/Diary/Stats. Вибрана — заливка Surface/surface-brand (#006944) з іконкою й підписом
 * Surface/surface-brand-light, невибрані — суцільні білі (Surface/surface-card) з сірою іконкою. Іконки — `TeperaIcons` (SVG 1:1 з компонента "Navbar icons", node
 * 274:484). Додавання часу лишається per-категорійним (HomeScreen.CategoryCard).
 */
/**
 * Снекбар після збереження: тихий (Monastic Style), без анімованих акцентів. Зникає сам через 4 с;
 * "Скасувати" видаляє щойно створений запис (усі його частини, якщо активність через дві доби).
 * Над нижнім навбаром, щоб не перекривати його.
 */
@Composable
private fun UndoSavedSnackbar(
    pending: SavedEntryUndo.Pending,
    activityRepository: ActivityRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(pending) {
        delay(4_000)
        SavedEntryUndo.clear()
    }
    Row(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TeperaPalette.dialogSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.add_entry_saved_snackbar),
            style = MaterialTheme.typography.bodyLarge,
            color = TeperaPalette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        TeperaButton(
            text = stringResource(R.string.dialog_cancel),
            onClick = {
                scope.launch {
                    pending.ids.forEach { id ->
                        activityRepository.getById(id)?.let { activityRepository.deleteWholeActivity(it) }
                    }
                }
                SavedEntryUndo.clear()
            },
            size = TeperaButtonSize.Small,
            type = TeperaButtonType.Secondary
        )
    }
}

@Composable
private fun TeperaBottomNavBar(
    currentRoute: String?,
    navController: NavHostController,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    onReselected: (String) -> Unit
) {
    // enableEdgeToEdge() (MainActivity) малює контент ПІД системними барами — без урахування
    // WindowInsets.navigationBars "таблетка" на фіксованому bottom-відступі ховалась під
    // системним навбаром на пристроях з високим 3-кнопковим навбаром (підтверджено на Huawei
    // P9, EMUI) — на Samsung S23 із жестовою навігацією (тонша смуга) цього не було помітно.
    // 8.dp зверху системного інсету — той самий подих, що раніше давав фіксований 24.dp.
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier = modifier
            .fillMaxWidth()
            // За прямим запитом користувача піднято на 8dp (було 8.dp над системним інсетом, тепер 16.dp).
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp + navigationBarInset)
            .height(62.dp)
            .clip(RoundedCornerShape(32.dp))
            // Матове скло: сильніше розмиття (прибирає деталі контенту під навбаром, тож вкладки читаються
            // чіткіше) + легкий шум, поверх — напівпрозорий navPillCard (білий 30%) і тонка світла кромка скла.
            // До Android 12 (API < 31) розмиття недоступне — Haze тоді малює лише fallbackTint; без нього контент
            // проглядає крізь скло (підтверджено на Huawei P9, Android 8), тож там фон щільніший.
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = null,
                    blurRadius = 40.dp,
                    noiseFactor = 0.12f,
                    // Темна тема: темне скло (іконки невибраних вкладок світлі), світла — біле.
                    fallbackTint = HazeTint(
                        if (com.serkodesign.tepera.ui.theme.LocalTeperaColors.current.isDark) Color(0xFF16231F).copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.9f)
                    )
                )
            )
            .background(TeperaPalette.navPillCard)
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(32.dp))
            .padding(6.dp),
        // Відступ між таблетками = внутрішній відступ контуру (6dp), як у макеті (gap-[6px]).
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        // Figma "App concept" node 274:369: три рівні частки без проміжків (justify-between).
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavPillTab(
            icon = if (currentRoute == Routes.HOME) TeperaIcons.HomeFilled else TeperaIcons.HomeOutlined,
            label = stringResource(R.string.home_screen_title),
            selected = currentRoute == Routes.HOME,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            // Дзеркально до останньої вкладки: зовнішній (лівий) бік 40, внутрішній 16.
            unselectedShape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp, topEnd = 16.dp, bottomEnd = 16.dp),
            onClick = {
                if (currentRoute != Routes.HOME) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    onReselected(Routes.HOME)
                }
            }
        )
        NavPillTab(
            icon = if (currentRoute == Routes.DIARY) TeperaIcons.BallotFilled else TeperaIcons.BallotOutlined,
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
                } else {
                    onReselected(Routes.DIARY)
                }
            }
        )
        NavPillTab(
            icon = if (currentRoute == Routes.STATS) TeperaIcons.LeaderboardFilled else TeperaIcons.LeaderboardOutlined,
            label = stringResource(R.string.stats_nav_action),
            selected = currentRoute == Routes.STATS,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            unselectedShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 40.dp, bottomEnd = 40.dp),
            onClick = {
                if (currentRoute != Routes.STATS) {
                    navController.navigate(Routes.STATS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    onReselected(Routes.STATS)
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
    modifier: Modifier = Modifier,
    unselectedShape: Shape = RoundedCornerShape(16.dp)
) {
    // Оформлення за Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 395:1011: вибрана — заливка
    // Surface/surface-brand (#006944), радіус 40, іконка й підпис Surface/surface-brand-light (#DCF6ED);
    // невибрана — суцільна біла заливка (Surface/surface-card), радіус 16, сіра (#505050) іконка. Анімація M3 (emphasized): колір заливки й
    // вмісту та поява/зникнення підпису — розтягування по ширині + fade.
    val contentColor by animateColorAsState(
        if (selected) TeperaPalette.navPillSelectedContent else TeperaPalette.navPillUnselectedIcon,
        TeperaSpecs.effects(), label = "navTabContent"
    )
    // Невибрана таблетка за макетом (347:3151): F0F3F4 з прозорістю 10%, радіус 16 (вибрана — 40).
    val fill by animateColorAsState(
        if (selected) TeperaPalette.navPillSelected else TeperaPalette.navTabUnselectedFill,
        TeperaSpecs.effects(), label = "navTabFill"
    )
    // Макет 347:3146/3155: вибрана — 40 з усіх боків; крайні невибрані мають 40 з ЗОВНІШНЬОГО боку
    // контейнера (Home — ліворуч, Stats — праворуч), внутрішній бік 16; середня — 16 з усіх боків.
    val shape = if (selected) RoundedCornerShape(40.dp) else unselectedShape
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(fill)
            // Вкладка з роллю Tab і станом selected — скрінрідер озвучує "вкладка, вибрано" (WCAG 4.1.2).
            .selectable(selected = selected, role = androidx.compose.ui.semantics.Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = if (selected) null else label, tint = contentColor)
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(TeperaMotion.SHORT4, easing = TeperaMotion.EmphasizedDecelerate)) +
                    expandHorizontally(TeperaSpecs.spatial()),
                exit = fadeOut(tween(TeperaMotion.SHORT3, easing = TeperaMotion.EmphasizedAccelerate)) +
                    shrinkHorizontally(TeperaSpecs.spatial())
            ) {
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

/** Перехід між двома вкладками навбару (Home/Diary/Stats) — для них M3 "fade through". */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTopLevelSwitch(): Boolean =
    initialState.destination.route in Routes.BOTTOM_NAV_ROUTES && targetState.destination.route in Routes.BOTTOM_NAV_ROUTES
