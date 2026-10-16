package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import androidx.compose.foundation.layout.heightIn
import com.serkodesign.tepera.ui.theme.bottomNavClearance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.serkodesign.tepera.ui.theme.TeperaDialog

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.serkodesign.tepera.ui.category.CategoryViewModel
import com.serkodesign.tepera.ui.category.CreateCategoryDialog
import com.serkodesign.tepera.ui.category.CreateCategoryResult
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonSize
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.cards.CardSource
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.GateEventRepository
import com.serkodesign.tepera.data.repository.PatternRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryGlyphColor
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.pattern.PatternViewModel
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaMotion
import com.serkodesign.tepera.ui.theme.LegacyTeperaColors
import com.serkodesign.tepera.ui.theme.LocalTeperaColors
import androidx.compose.ui.platform.LocalConfiguration
import java.text.SimpleDateFormat
import java.util.Date
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
    pauseRepository: PauseRepository,
    patternRepository: PatternRepository,
    settingsStore: SettingsStore,
    sleepWindowRepository: SleepWindowRepository,
    userEstimateRepository: UserEstimateRepository,
    unlockRepository: UnlockRepository,
    activeTimerStore: ActiveTimerStore,
    cardHistoryRepository: CardHistoryRepository,
    gateEventRepository: GateEventRepository,
    onOpenSettings: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenStats: () -> Unit,
    onAddEntryForCategory: (String) -> Unit,
    onOpenCategoryHistory: (String) -> Unit,
    onShowOnboarding: () -> Unit,
    onShowCategoryOnboarding: () -> Unit,
    onShowOnlineEstimateOnboarding: () -> Unit,
    onShowTargetOnboarding: () -> Unit,
    onShowWidgetSuggestion: () -> Unit,
    onShowGatesOnboarding: () -> Unit,
    // Повторний тап по вкладці "Головна" в навбарі, коли вже на ній (TeperaNavHost) — той самий
    // принцип, що й на Щоденнику/Статистиці: значення саме по собі не важливе, лише його ЗМІНА
    // (кожен тап на вже вибраній вкладці інкрементує лічильник у NavHost) прокручує сюди наверх.
    scrollToTopSignal: Int = 0,
    // Тап по сповіщенню "підсумок тижня": оновити картку «Цей тиждень» і прокрутити карусель до неї.
    weeklySummaryNonce: Long? = null
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(categoryRepository, activityRepository, activeTimerStore)
    )
    val summary by viewModel.todaySummary.collectAsState()
    val summaryLoaded by viewModel.summaryLoaded.collectAsState()

    // За прямим запитом користувача: активна категорія переїжджає на початок сітки замість
    // окремого рядка "Зараз іде: X" над нею (ActiveTimerBar прибрано). Порядок — знімок, а не
    // жива реакція на trackingStartTime: перераховується лише при поверненні на Home
    // (resumeGeneration нижче), тож тап по play/pause НЕ смикає сітку під час самого перегляду
    // екрана. Категорія лишається зверху, навіть якщо таймер зупинили тим самим сеансом — до
    // наступного відкриття Home.
    var pinnedCategoryId by remember { mutableStateOf<String?>(null) }
    // resumeGeneration + capturedGeneration (не читання summary прямо в LifecycleResumeEffect) —
    // бо todaySummary вантажиться асинхронно (Room/DataStore, StateFlow стартує з emptyList()):
    // на холодному старті з уже активним таймером (напр. запущеним з віджета, або лишеним із
    // минулого сеансу) ON_RESUME спрацьовував РАНІШЕ, ніж перший реальний emit Flow — і брав
    // знімок з ще порожнього списку. Реальний баг, знайдений живим тестом на пристрої. Фікс:
    // резюм лише позначає "потрібен новий знімок" (resumeGeneration++), а окремий ефект чекає,
    // доки summary справді завантажиться (isNotEmpty), і бере знімок РІВНО раз на це покоління —
    // наступні зміни summary в межах того самого покоління (напр. зупинка таймера) вже ігноруються.
    var resumeGeneration by remember { mutableStateOf(0) }
    var capturedGeneration by remember { mutableStateOf(-1) }
    LaunchedEffect(summary, resumeGeneration) {
        if (summary.isNotEmpty() && capturedGeneration != resumeGeneration) {
            pinnedCategoryId = summary.firstOrNull { it.trackingStartTime != null }?.category?.id
            capturedGeneration = resumeGeneration
        }
    }
    val orderedSummary = remember(summary, pinnedCategoryId) {
        val pinnedId = pinnedCategoryId
        if (pinnedId == null) {
            summary
        } else {
            val (pinned, rest) = summary.partition { it.category.id == pinnedId }
            pinned + rest
        }
    }

    // Кнопка "+ Додати" біля "Активності": створення власної категорії, та сама логіка й діалог,
    // що в Налаштування → Категорії.
    val categoryViewModel: CategoryViewModel = viewModel(factory = CategoryViewModel.Factory(categoryRepository))
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val createCategoryResult by categoryViewModel.createResult.collectAsState()
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showCategoryLimitNotice by remember { mutableStateOf(false) }
    val customSlotAvailable = allCategories.count { it.isCustom } < CategoryViewModel.MAX_CUSTOM_CATEGORIES
    LaunchedEffect(createCategoryResult) {
        when (createCategoryResult) {
            CreateCategoryResult.Success -> {
                showCreateCategoryDialog = false
                categoryViewModel.consumeCreateResult()
            }
            CreateCategoryResult.LimitReached -> {
                showCreateCategoryDialog = false
                showCategoryLimitNotice = true
                categoryViewModel.consumeCreateResult()
            }
            null -> Unit
        }
    }
    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onSave = { name, icon, color -> categoryViewModel.createCustomCategory(name, icon, color) }
        )
    }
    if (showCategoryLimitNotice) {
        TeperaDialog(
            onDismissRequest = { showCategoryLimitNotice = false },
            text = stringResource(R.string.category_custom_limit_reached),
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = { showCategoryLimitNotice = false }
        )
    }

    // CC-9: м'яка пропозиція оновитись (Firebase Remote Config) — незалежна від ContextCardStack/
    // CardEngine (не "картка даних" із тижневим бюджетом, а системний нагад, що має право з'явитись
    // при кожному відкритті Home, доки людина не оновиться або не закриє саме ЦЮ версію).
    val updateViewModel: UpdateViewModel = viewModel(
        factory = UpdateViewModel.Factory(LocalContext.current.applicationContext, settingsStore)
    )
    val updateState by updateViewModel.uiState.collectAsState()
    if (updateState.visible) {
        TeperaDialog(
            onDismissRequest = updateViewModel::dismiss,
            title = stringResource(R.string.update_dialog_title),
            text = updateState.whatsNew.ifBlank { stringResource(R.string.update_dialog_fallback_text) },
            confirmText = stringResource(R.string.update_dialog_confirm),
            onConfirm = updateViewModel::openStoreListing,
            dismissText = stringResource(R.string.update_dialog_dismiss),
            onDismiss = updateViewModel::dismiss
        )
    }

    val balanceViewModel: BalanceViewModel = viewModel(
        factory = BalanceViewModel.Factory(balanceRepository, activityRepository, categoryRepository, settingsStore, sleepWindowRepository)
    )
    val balanceState by balanceViewModel.uiState.collectAsState()

    val weeklyReflectionViewModel: WeeklyReflectionViewModel = viewModel(
        factory = WeeklyReflectionViewModel.Factory(settingsStore, balanceRepository, cardHistoryRepository)
    )
    val weeklyReflectionState by weeklyReflectionViewModel.uiState.collectAsState()

    // FR-D.1–D.7 (SRS v2.6): сканування пауз через queryEvents() — on-demand при кожному
    // відкритті Home (не фонова WorkManager-задача), сам PauseViewModel мовчить поза дозволеним
    // вікном опитування (FR-D.3), тож зайвого сканування поза ним не відбувається.
    val pauseViewModel: PauseViewModel = viewModel(
        factory = PauseViewModel.Factory(pauseRepository, balanceRepository, activityRepository, settingsStore, sleepWindowRepository)
    )
    val pauseState by pauseViewModel.uiState.collectAsState()

    // FR-D.8/D.9: тепловий патерн доби — власний інстанс на Home (Stats має свій, з тими самими
    // Repository, але окремим refresh-циклом). Живить лише теплову сітку у шторці підсумків (вкладка "Вчора").
    val patternViewModel: PatternViewModel = viewModel(
        // Home показує патерн ВЧОРАШНЬОЇ доби (1 день), не середнє за тиждень.
        factory = PatternViewModel.Factory(
            patternRepository, balanceRepository, settingsStore, initialPeriodDays = 1,
            sleepWindowRepository = sleepWindowRepository
        )
    )
    val patternState by patternViewModel.uiState.collectAsState()

    // Home FINAL: тізери "Вчора / Тиждень / Місяць" у hero і шторка підсумків (docs/HANDOFF.md, розділи 5 і 8).
    val summaryViewModel: HomeSummaryViewModel = viewModel(
        factory = HomeSummaryViewModel.Factory(
            (LocalContext.current.applicationContext as com.serkodesign.tepera.TeperaApp).daySummaryRepository
        )
    )
    val summaryDays by summaryViewModel.days.collectAsState()
    var summarySheetTab by remember { mutableStateOf<SummaryTab?>(null) }

    // T-3 (tepera-dev-spec.md), крок 5 "Повернення → реальне число поруч із оцінкою": один
    // механізм покриває і негайне повернення з системних Налаштувань, і випадок "дозвіл з'явився
    // набагато пізніше" (акцептанс-критерій) — refresh() шукає найновішу нерозв'язану оцінку
    // щоразу, коли відкривається Home.
    val onlineEstimateRevealViewModel: OnlineEstimateRevealViewModel = viewModel(
        factory = OnlineEstimateRevealViewModel.Factory(userEstimateRepository, balanceRepository, settingsStore)
    )
    val onlineEstimateRevealState by onlineEstimateRevealViewModel.uiState.collectAsState()

    // T-14 (tepera-dev-spec.md): "Скільки разів, по-твоєму, ти вчора розблоковував телефон?" —
    // той самий одноразовий init-check, що WeeklyReflectionViewModel (не в LifecycleResumeEffect
    // нижче, той самий свідомий вибір, що вже застосований до weeklyReflectionViewModel).
    val unlockEstimateViewModel: UnlockEstimateViewModel = viewModel(
        factory = UnlockEstimateViewModel.Factory(
            unlockRepository, balanceRepository, sleepWindowRepository,
            userEstimateRepository, settingsStore, cardHistoryRepository
        )
    )
    val unlockEstimateState by unlockEstimateViewModel.uiState.collectAsState()

    // T-10 (tepera-dev-spec.md): "О котрій ти вчора востаннє брав телефон?" — замінює прибраний
    // ПОСТІЙНИЙ показ (LastPhoneUseCard, FR-D.7, видалено). Той самий одноразовий init-check, що
    // weeklyReflectionViewModel/unlockEstimateViewModel вище.
    val lastPhoneUseEstimateViewModel: LastPhoneUseEstimateViewModel = viewModel(
        factory = LastPhoneUseEstimateViewModel.Factory(
            pauseRepository, balanceRepository, userEstimateRepository, settingsStore, cardHistoryRepository
        )
    )
    val lastPhoneUseEstimateState by lastPhoneUseEstimateViewModel.uiState.collectAsState()

    // T-6 (tepera-dev-spec.md), FR-P.3: "цього місяця N разів ти вирішив не зараз" (ворота, T-5).
    // Той самий одноразовий init-check, що решта карток вище.
    val gateEventsSummaryViewModel: GateEventsSummaryViewModel = viewModel(
        factory = GateEventsSummaryViewModel.Factory(gateEventRepository, cardHistoryRepository)
    )
    val gateEventsSummaryState by gateEventsSummaryViewModel.uiState.collectAsState()
    val welcomeBackViewModel: WelcomeBackViewModel = viewModel(
        factory = WelcomeBackViewModel.Factory(
            (LocalContext.current.applicationContext as com.serkodesign.tepera.TeperaApp).welcomeBackRepository,
            settingsStore,
            cardHistoryRepository
        )
    )
    val welcomeBackState by welcomeBackViewModel.uiState.collectAsState()

    // T-13 (tepera-dev-spec.md): "рушій карток" — вирішує, яку саме множину з готових-до-показу
    // карток (isDue/visible нижче) реально видно на екрані, застосовуючи глобальний бюджет
    // (не більше 1 картки-оцінки на тиждень, максимум 3 картки одночасно, "подієві не витісняють
    // тижневі більш ніж двічі поспіль"). Кожна ViewModel і далі рахує лише ГОТОВНІСТЬ ДАНИХ
    // (isDue/visible), не саму появу — appear-рішення повністю тут.
    val cardStackViewModel: CardStackViewModel = viewModel(
        factory = CardStackViewModel.Factory(cardHistoryRepository)
    )
    val visibleCards by cardStackViewModel.visibleCards.collectAsState()

    // T-2 (tepera-dev-spec.md): "одразу після надання дозволу обробити всю доступну історію" —
    // одноразовий бекфіл DetectedGapEntity за минулі дні, щойно доступ підтверджено вперше.
    val backfillViewModel: BackfillViewModel = viewModel(
        factory = BackfillViewModel.Factory(balanceRepository, pauseRepository, sleepWindowRepository, settingsStore)
    )
    val backfillState by backfillViewModel.uiState.collectAsState()

    // Доступ до статистики використання надається в системних Налаштуваннях, поза застосунком —
    // без цього ефекту повернення з Налаштувань не оновило б картку без ручного перезаходу на Home.
    LifecycleResumeEffect(Unit) {
        balanceViewModel.refresh()
        pauseViewModel.refresh()
        patternViewModel.refresh()
        summaryViewModel.refresh()
        onlineEstimateRevealViewModel.refresh()
        backfillViewModel.runIfNeeded()
        resumeGeneration++
        onPauseOrDispose { }
    }

    // T-13: перерахунок видимої множини карток щоразу, коли готовність БУДЬ-ЯКОЇ з них
    // змінюється (не лише при відкритті Home — напр. "Гаразд"/"×" на картці одразу звільняє
    // місце для наступної в черзі, без очікування наступного LifecycleResumeEffect).
    LaunchedEffect(
        onlineEstimateRevealState, pauseState, weeklyReflectionState, unlockEstimateState,
        lastPhoneUseEstimateState, gateEventsSummaryState, welcomeBackState
    ) {
        cardStackViewModel.evaluate(
            listOf(
                CardSource(CardType.WELCOME_BACK, priority = -1, minIntervalDays = null, dataReady = welcomeBackState.visible),
                CardSource(CardType.ONLINE_ESTIMATE_REVEAL, priority = 0, minIntervalDays = null, dataReady = onlineEstimateRevealState.visible),
                CardSource(CardType.PAUSE, priority = 1, minIntervalDays = null, dataReady = pauseState.visible),
                CardSource(CardType.WEEKLY_REFLECTION, priority = 2, minIntervalDays = 7, dataReady = weeklyReflectionState.isDue),
                CardSource(CardType.UNLOCK_ESTIMATE, priority = 3, minIntervalDays = 14, dataReady = unlockEstimateState.isDue),
                CardSource(CardType.LAST_PHONE_USE_ESTIMATE, priority = 4, minIntervalDays = 14, dataReady = lastPhoneUseEstimateState.isDue),
                CardSource(CardType.GATE_EVENTS_SUMMARY, priority = 7, minIntervalDays = 30, dataReady = gateEventsSummaryState.isDue)
            )
        )
    }

    // T-8 (tepera-dev-spec.md), крок 2 "Порядку першого запуску": першим кроком онбордингу, перед
    // онбординг-оцінкою Online-часу — вибір "що саме відмічатимеш"
    // (документ: попередні 5 категорій самі по собі норма, FR-P.5).
    val categoryOnboardingSeen by settingsStore.categoryOnboardingSeen.collectAsState(initial = true)
    LaunchedEffect(categoryOnboardingSeen) {
        if (!categoryOnboardingSeen) {
            onShowCategoryOnboarding()
        }
    }

    // T-3, крок 3 "Порядку першого запуску": одразу після вибору категорій (T-8), ще до
    // пояснення дозволу нижче — не залежить від стану доступу до статистики, той самий принцип.
    val onlineEstimateOnboardingSeen by settingsStore.onlineEstimateOnboardingSeen.collectAsState(initial = true)
    LaunchedEffect(categoryOnboardingSeen, onlineEstimateOnboardingSeen) {
        if (categoryOnboardingSeen && !onlineEstimateOnboardingSeen) {
            onShowOnlineEstimateOnboarding()
        }
    }

    // FR-7.1: онбординг доступу до статистики — лише ПІСЛЯ того, як вибір
    // категорій (T-8) і онбординг-оцінка Online-часу (T-3) вже показані, інакше кілька ефектів
    // могли б спробувати навігувати одночасно на першому запуску.
    val onboardingSeen by settingsStore.onboardingUsageAccessSeen.collectAsState(initial = true)
    LaunchedEffect(balanceState.hasUsageAccess, onboardingSeen, categoryOnboardingSeen, onlineEstimateOnboardingSeen) {
        if (categoryOnboardingSeen && onlineEstimateOnboardingSeen && balanceState.hasUsageAccess == false && !onboardingSeen) {
            onShowOnboarding()
        }
    }

    // Figma user-flow (k6s4prQ9oK9x2uUvzHRghR, node 14:791): "Пропозиція віджета" — останній
    // крок онбордингу, ОБИДВІ гілки "доступ надано? так/ні" сходяться сюди. "Крок дозволу
    // розв'язаний" — доступ уже надано (permissionScreen вище й не показувався) АБО сам
    // permission-екран уже показувався (onboardingSeen), незалежно від того, чим скінчилось.
    // CC-1: крок "Орієнтир на день" — ПІСЛЯ кроку дозволу й лише коли доступ до статистики реально є
    // (потрібна історія для власного середнього). Той самий race "Home оживає між popBackStack()/navigate()",
    // що й нижче, тож перед переходом невелика затримка.
    val targetOnboardingSeen by settingsStore.targetOnboardingSeen.collectAsState(initial = true)
    LaunchedEffect(balanceState.hasUsageAccess, categoryOnboardingSeen, onlineEstimateOnboardingSeen, targetOnboardingSeen) {
        if (categoryOnboardingSeen && onlineEstimateOnboardingSeen &&
            balanceState.hasUsageAccess == true && !targetOnboardingSeen
        ) {
            delay(1000)
            onShowTargetOnboarding()
        }
    }
    // Пропозиція віджета чекає на цей крок, поки він реально належить до ланцюжка (є доступ і крок ще не пройдено).
    val targetStepDone = targetOnboardingSeen || balanceState.hasUsageAccess == false

    val widgetSuggestionSeen by settingsStore.widgetSuggestionSeen.collectAsState(initial = true)
    LaunchedEffect(balanceState.hasUsageAccess, onboardingSeen, categoryOnboardingSeen, onlineEstimateOnboardingSeen, widgetSuggestionSeen, targetStepDone) {
        val permissionStepResolved = balanceState.hasUsageAccess == true || onboardingSeen
        if (categoryOnboardingSeen && onlineEstimateOnboardingSeen && permissionStepResolved && targetStepDone && !widgetSuggestionSeen) {
            // Race "Home оживає між popBackStack()/navigate()": OnboardingScreen
            // (пояснення дозволу) виставляє onboardingSeen=true у своєму LaunchedEffect(Unit)
            // ОДРАЗУ при монтуванні, не чекаючи дії користувача — і Home встигає прочитати це
            // на тому самому короткому "оживанні" між popBackStack()/navigate(), перш ніж
            // OnboardingScreen встигає реально лишитись на екрані. Без затримки цей ефект
            // стрибав одразу на WidgetSuggestionScreen, повністю пропускаючи екран пояснення
            // дозволу (знайдено живим тестом на Samsung S23, T-3 переставав показуватись).
            delay(1000)
            onShowWidgetSuggestion()
        }
    }

    // Останній крок онбордингу (за прямим запитом користувача) — коротка згадка про ворота
    // ("Застосунки з затримкою"), ПІСЛЯ пропозиції віджета. Та сама затримка й та сама причина
    // (race "Home оживає між popBackStack()/navigate()"), що й крок вище.
    val gatesOnboardingSeen by settingsStore.gatesOnboardingSeen.collectAsState(initial = true)
    LaunchedEffect(
        balanceState.hasUsageAccess, onboardingSeen, categoryOnboardingSeen,
        onlineEstimateOnboardingSeen, widgetSuggestionSeen, targetStepDone, gatesOnboardingSeen
    ) {
        val permissionStepResolved = balanceState.hasUsageAccess == true || onboardingSeen
        if (categoryOnboardingSeen && onlineEstimateOnboardingSeen && permissionStepResolved &&
            targetStepDone && widgetSuggestionSeen && !gatesOnboardingSeen
        ) {
            delay(1000)
            onShowGatesOnboarding()
        }
    }

    val context = LocalContext.current

    val scrollState = rememberScrollState()
    // Тап по сповіщенню "підсумок тижня": відкриває шторку підсумків на вкладці "Тиждень" (картки "Цей тиждень" на
    // Home більше нема).
    LaunchedEffect(weeklySummaryNonce) {
        if (weeklySummaryNonce != null) summarySheetTab = SummaryTab.WEEK
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal != 0) scrollState.animateScrollTo(0)
    }

    // Прозорий containerColor: градієнтний фон малює зовнішній Box у TeperaNavHost (а не тут) —
    // інакше він потрапляє під contentPadding зовнішнього Scaffold і не сягає країв екрана.
    Scaffold(containerColor = Color.Transparent) { padding ->
        // Увесь екран скролиться ОДНИМ контейнером (за прямим запитом користувача) — раніше
        // скролилась лише сітка категорій (Modifier.weight(1f) + LazyVerticalGrid), а заголовок,
        // контекстні картки й картка "Мій день" лишались фіксованими зверху. На малих екранах
        // (Samsung S23, Huawei P9) це стискало сітку категорій у замалу область, і остання картка
        // під час прокрутки візуально "обрізалась" на межі цієї стиснутої області та знову на
        // межі напівпрозорої навбар-"таблетки" знизу. Один спільний Modifier.verticalScroll
        // прибирає внутрішню межу зверху (немає окремого фіксованого/скрольованого стику), а
        // Spacer(100.dp) в самому кінці — той самий запас, що раніше був bottom-паддінгом сітки,
        // гарантує, що остання картка повністю прокручується НАД "таблеткою", а не впирається в неї.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Home FINAL (docs/HANDOFF.md): hero-картка замінила шапку з датою й слайдер "Мій день / Патерн /
            // Цей тиждень". Орб, смуга дня й слот тізерів (етап 4). Контекстні картки — під списком активностей.
            val heroState = rememberHeroState(balanceState)
            val today = remember(heroState) { java.time.LocalDate.now() }
            val todayOffline = (heroState as? HeroUiState.Data)?.offlineMinutes
            // Запасний варіант слота (HANDOFF, п. 5.9): шрифт ≥ 130% або ширина < 340dp — одна широка плитка.
            val configuration = LocalConfiguration.current
            val forceWideTeaser = configuration.fontScale >= 1.3f || configuration.screenWidthDp < 340
            val teaserSlot = remember(summaryDays, todayOffline, forceWideTeaser, today) {
                buildTeaserSlot(
                    yesterday = summaryDays[today.minusDays(1)],
                    week = buildPeriod(today, SUMMARY_WEEK_DAYS, summaryDays, todayOffline),
                    month = buildPeriod(today, SUMMARY_MONTH_DAYS, summaryDays, todayOffline),
                    today = today,
                    forceWide = forceWideTeaser
                )
            }
            HomeHero(
                state = heroState,
                onOpenSettings = onOpenSettings,
                onOpenKnowledgeBase = onOpenKnowledgeBase,
                onGrantAccess = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                teaser = { SummaryTeaserSlot(slot = teaserSlot, onOpen = { summarySheetTab = it }) }
            )
            summarySheetTab?.let { initialTab ->
                SummarySheet(
                    initialTab = initialTab,
                    today = today,
                    completed = summaryDays,
                    todayOffline = todayOffline,
                    todayOnline = (heroState as? HeroUiState.Data)?.onlineMinutes,
                    yesterdayHourlyMinutes = if (patternState.hasEnoughData) patternState.hourlyMinutes else null,
                    onOpenStats = {
                        summarySheetTab = null
                        onOpenStats()
                    },
                    onDismiss = { summarySheetTab = null }
                )
            }

            // T-2 (tepera-dev-spec.md): "обробка... з індикатором" — короткий тихий рядок, доки
            // триває одноразовий бекфіл історії пауз (BackfillViewModel), зазвичай зникає
            // за долі секунди.
            if (backfillState.isProcessing) {
                Text(
                    text = stringResource(R.string.home_backfill_processing),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // "Патерн вчора" і "Цей тиждень" з Home прибрано (рішення власника, 15.10.2026): патерн — у шторці
            // підсумків (вкладка "Вчора") і на Статистиці, а "Цей тиждень" у макеті Home Final не передбачений.
            // FR-D.10/D.10a/D.11: вертикальний стек контекстних карток, пріоритизований за актуальністю —
            // кожна сама вирішує, чи їй бути видимою. Розміщено НАД списком активностей (16.10.2026, за запитом власника).
            ContextCardStack(
                visibleCards = visibleCards,
                onlineEstimateRevealState = onlineEstimateRevealState,
                onDismissOnlineEstimateReveal = onlineEstimateRevealViewModel::dismiss,
                pauseState = pauseState,
                categories = summary.map { it.category },
                onLabelGap = pauseViewModel::labelGap,
                onDismissGap = pauseViewModel::dismissGap,
                onDismissPauseCard = pauseViewModel::dismissCard,
                weeklyState = weeklyReflectionState,
                onSelectGuess = weeklyReflectionViewModel::selectGuess,
                onDismissWeekly = weeklyReflectionViewModel::dismiss,
                unlockEstimateState = unlockEstimateState,
                onSelectUnlockGuess = unlockEstimateViewModel::selectGuess,
                onDismissUnlockEstimate = unlockEstimateViewModel::dismiss,
                lastPhoneUseEstimateState = lastPhoneUseEstimateState,
                onSelectLastPhoneUseGuess = lastPhoneUseEstimateViewModel::selectGuess,
                onDismissLastPhoneUseEstimate = lastPhoneUseEstimateViewModel::dismiss,
                gateEventsSummaryState = gateEventsSummaryState,
                onDismissGateEventsSummary = gateEventsSummaryViewModel::dismiss,
                welcomeBackState = welcomeBackState,
                onDismissWelcomeBack = welcomeBackViewModel::dismiss
            )

            // Те саме, що Налаштування → Категорії → "Додати свою категорію" (FR-2.2, ліміт 2).
            // На ліміті кнопка лишається натискною — тап пояснює, чому додати не можна (не `enabled = false`).
            ActivitiesSectionHeader(
                title = stringResource(R.string.activities_title),
                actionLabel = stringResource(R.string.home_add_category),
                onAction = { if (customSlotAvailable) showCreateCategoryDialog = true else showCategoryLimitNotice = true }
            )

            if (!summaryLoaded) {
                // Скелетони `ActivityRow Type=Loading` (HANDOFF, розділ 6), доки список категорій вантажиться.
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { ActivityRowSkeleton() }
                }
            } else if (summary.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.home_no_entries_today))
                }
            } else {
                // Горизонтальні рядки (Home FINAL): активний рядок закріплено першим (знімок при поверненні на
                // Home, див. pinnedCategoryId вище — тап по play не смикає список під час перегляду).
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    orderedSummary.forEach { item ->
                        ActivityRow(
                            item = item,
                            onToggleTimer = { viewModel.toggleTimer(item.category.id) },
                            onAddTime = { onAddEntryForCategory(item.category.id) },
                            onOpenHistory = { onOpenCategoryHistory(item.category.id) }
                        )
                    }
                }
            }

            // Запас під напівпрозору навбар-"таблетку" знизу (той самий 100.dp, що раніше був
            // bottom-паддінгом сітки) — гарантує, що остання картка прокручується НАД нею, а не
            // впирається в неї впритул.
            Spacer(modifier = Modifier.height(bottomNavClearance()))
        }
    }
}
