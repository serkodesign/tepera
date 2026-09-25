package com.serkodesign.tepera.ui.home

import com.serkodesign.tepera.ui.theme.TeperaDialog

import android.content.Intent
import android.os.Build
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.draw.shadow
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
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.pattern.PatternViewModel
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.TeperaMotion
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
    onAddEntryForCategory: (String) -> Unit,
    onOpenCategoryHistory: (String) -> Unit,
    onShowOnboarding: () -> Unit,
    onShowCategoryOnboarding: () -> Unit,
    onShowOnlineEstimateOnboarding: () -> Unit,
    onShowTargetOnboarding: () -> Unit,
    onShowWidgetSuggestion: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(categoryRepository, activityRepository, activeTimerStore)
    )
    val summary by viewModel.todaySummary.collectAsState()

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

    // Досліджено з Figma-макета (node 2062:2862, "This week") — тижневий дайджест лічильників,
    // окрема картка стеку (ContextCardStack.kt), не в SRS буквально.
    val weeklyDigestViewModel: WeeklyDigestViewModel = viewModel(
        factory = WeeklyDigestViewModel.Factory(activityRepository, balanceRepository, settingsStore, sleepWindowRepository)
    )
    val weeklyDigestState by weeklyDigestViewModel.uiState.collectAsState()

    // FR-D.8/D.9: тепловий патерн доби — власний інстанс на Home (Stats має свій, з тими самими
    // Repository, але окремим refresh-циклом).
    val patternViewModel: PatternViewModel = viewModel(
        // Home показує патерн ВЧОРАШНЬОЇ доби (1 день), не середнє за тиждень.
        factory = PatternViewModel.Factory(
            patternRepository, balanceRepository, settingsStore, initialPeriodDays = 1,
            sleepWindowRepository = sleepWindowRepository
        )
    )
    val patternState by patternViewModel.uiState.collectAsState()

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
        weeklyDigestViewModel.refresh()
        onlineEstimateRevealViewModel.refresh()
        backfillViewModel.runIfNeeded()
        onPauseOrDispose { }
    }

    // T-13: перерахунок видимої множини карток щоразу, коли готовність БУДЬ-ЯКОЇ з них
    // змінюється (не лише при відкритті Home — напр. "Гаразд"/"×" на картці одразу звільняє
    // місце для наступної в черзі, без очікування наступного LifecycleResumeEffect).
    LaunchedEffect(
        onlineEstimateRevealState, pauseState, weeklyReflectionState, unlockEstimateState,
        lastPhoneUseEstimateState, weeklyDigestState, patternState, gateEventsSummaryState, welcomeBackState
    ) {
        cardStackViewModel.evaluate(
            listOf(
                CardSource(CardType.WELCOME_BACK, priority = -1, minIntervalDays = null, dataReady = welcomeBackState.visible),
                CardSource(CardType.ONLINE_ESTIMATE_REVEAL, priority = 0, minIntervalDays = null, dataReady = onlineEstimateRevealState.visible),
                CardSource(CardType.PAUSE, priority = 1, minIntervalDays = null, dataReady = pauseState.visible),
                CardSource(CardType.WEEKLY_REFLECTION, priority = 2, minIntervalDays = 7, dataReady = weeklyReflectionState.isDue),
                CardSource(CardType.UNLOCK_ESTIMATE, priority = 3, minIntervalDays = 14, dataReady = unlockEstimateState.isDue),
                CardSource(CardType.LAST_PHONE_USE_ESTIMATE, priority = 4, minIntervalDays = 14, dataReady = lastPhoneUseEstimateState.isDue),
                CardSource(CardType.WEEKLY_DIGEST, priority = 5, minIntervalDays = null, dataReady = weeklyDigestState.visible),
                CardSource(CardType.PATTERN, priority = 6, minIntervalDays = null, dataReady = patternState.visible),
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
    val widgetSuggestionSeen by settingsStore.widgetSuggestionSeen.collectAsState(initial = true)
    LaunchedEffect(balanceState.hasUsageAccess, onboardingSeen, categoryOnboardingSeen, onlineEstimateOnboardingSeen, widgetSuggestionSeen, targetStepDone) {
        val permissionStepResolved = balanceState.hasUsageAccess == true || onboardingSeen
        if (categoryOnboardingSeen && onlineEstimateOnboardingSeen && permissionStepResolved && targetStepDone && !widgetSuggestionSeen) {
            // Race "Home оживає між popBackStack()/navigate()": OnboardingScreen
            // (пояснення дозволу) виставляє onboardingSeen=true у своєму LaunchedEffect(Unit)
            // ОДРАЗУ при монтуванні, не чекаючи дії користувача — і Home встигає прочитати це
            // на тому самому короткому "оживанні" між popBackStack()/navigate(), перш ніж
            // OnboardingScreen встигає реально лишитись на екрані. Без затримки цей ефект
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

            // стрибав одразу на WidgetSuggestionScreen, повністю пропускаючи екран пояснення
            // дозволу (знайдено живим тестом на Samsung S23, T-3 переставав показуватись).
            delay(1000)
            onShowWidgetSuggestion()
        }
    }

    val context = LocalContext.current

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
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(onOpenSettings = onOpenSettings, onOpenKnowledgeBase = onOpenKnowledgeBase)

            // GAP-5: активний таймер (у т.ч. запущений з віджета) видно одразу й зупиняється звідси.
            summary.firstOrNull { it.trackingStartTime != null }?.let { active ->
                ActiveTimerBar(
                    active = active,
                    onStop = { viewModel.toggleTimer(active.category.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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

            // Горизонтальний слайдер (Figma "App concept" node 192:726): "Мій день" → "Патерн
            // вчора" → "Цей тиждень". Сторінки з даними додаються за тим самим рушієм карток
            // (`visibleCards`), що й раніше, коли ці картки були в вертикальному стеку.
            HomeCardsPager(
                pages = buildList<@Composable () -> Unit> {
                    add {
                        MyDayCard(
                            state = balanceState,
                            onOpenUsageAccessSettings = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            onLearnMore = onShowOnboarding
                        )
                    }
                    if (CardType.PATTERN in visibleCards) {
                        add { PatternMiniCard(state = patternState, onDismiss = patternViewModel::dismiss) }
                    }
                    if (CardType.WEEKLY_DIGEST in visibleCards) {
                        add { WeeklyDigestCard(state = weeklyDigestState, onDismiss = weeklyDigestViewModel::dismiss) }
                    }
                }
            )

            // FR-D.10/D.10a/D.11: вертикальний стек контекстних карток під слайдером,
            // пріоритизований за актуальністю — кожна сама вирішує, чи їй бути видимою.
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.activities_title),
                    fontFamily = TeperaPalette.headlineFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    color = TeperaPalette.buttonBrandDark
                )
                // Те саме, що Налаштування → Категорії → "Додати свою категорію" (FR-2.2, ліміт 2).
                // На ліміті кнопка виглядає неактивною (alpha), але лишається натискною — тап
                // пояснює, чому додати не можна (не `enabled = false`, той блокує тап взагалі).
                TeperaButton(
                    text = stringResource(R.string.home_add_category),
                    onClick = { if (customSlotAvailable) showCreateCategoryDialog = true else showCategoryLimitNotice = true },
                    size = TeperaButtonSize.Small,
                    textSizeOverride = 18.sp,
                    lineHeightOverride = 20.sp,
                    contentColorOverride = TeperaPalette.buttonBrand,
                    type = TeperaButtonType.Tertiary,
                    modifier = Modifier.alpha(if (customSlotAvailable) 1f else 0.5f)
                )
            }

            if (summary.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.home_no_entries_today))
                }
            } else {
                // 2 колонки, побудовані вручну по рядках (замість LazyVerticalGrid) — категорій
                // завжди небагато (до 6 дефолтних + 2 кастомні, FR-2.1/2.2, ліміт піднято в T-8),
                // а весь екран тепер
                // скролиться одним Modifier.verticalScroll вище, всередині якого lazy-контейнер
                // з Modifier.weight() непридатний (батько вимірює дітей з необмеженою висотою).
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    summary.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryCard(
                                        item = item,
                                        onToggleTimer = { viewModel.toggleTimer(item.category.id) },
                                        onAddTime = { onAddEntryForCategory(item.category.id) },
                                        onOpenHistory = { onOpenCategoryHistory(item.category.id) }
                                    )
                                }
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Запас під напівпрозору навбар-"таблетку" знизу (той самий 100.dp, що раніше був
            // bottom-паддінгом сітки) — гарантує, що остання картка прокручується НАД нею, а не
            // впирається в неї впритул.
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit, onOpenKnowledgeBase: () -> Unit) {
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
            color = TeperaPalette.buttonBrandDark,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = 27.sp
            )
        )
        // Figma "App concept" node 192:726: дві кнопки 44dp з асиметричними радіусами (ліва —
        // закруглена зліва 22/справа 8, права навпаки), біла заливка 80%, проміжок 4dp. Книга
        // відкриває "Базу знань" (перенесено з Налаштувань за запитом користувача).
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TeperaIconButton(
                icon = TeperaIcons.Book,
                contentDescription = stringResource(R.string.settings_knowledge_base_action),
                onClick = onOpenKnowledgeBase,
                shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                containerColor = TeperaPalette.headerButtonFill,
                contentColor = TeperaPalette.buttonBrandDark
            )
            TeperaIconButton(
                icon = TeperaIcons.Settings,
                contentDescription = stringResource(R.string.settings_nav_action),
                onClick = onOpenSettings,
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                containerColor = TeperaPalette.headerButtonFill,
                contentColor = TeperaPalette.buttonBrandDark
            )
        }
    }
}

/**
 * Картка категорії (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, node 192:726, "Activities").
 * Кружок-іконка й назва зверху, знизу кнопка play/pause (тап-таймер, HomeViewModel.toggleTimer) і
 * окрема "more_time" (ручне додавання часу САМЕ цій категорії, AddEntryScreen з попередньо
 * вибраною категорією).
 *
 * **Стан таймера (за запитом користувача):** коли таймер активний, картка стає суцільною
 * `#006944` з білим текстом 18sp, кнопка паузи розтягується на всю ширину, а "more_time"
 * зникає. Усі переходи — анімації Material 3 ([TeperaMotion]): колір/розмір тексту 500 мс по
 * "emphasized" кривій, вхід/вихід кнопки "more_time" — розширення/стиснення по ширині (500 мс)
 * плюс fade (декелерація на вході 200 мс, акселерація на виході 150 мс).
 * Тап по тілу картки (іконка/назва) відкриває повну історію категорії. Кастомна категорія в
 * неактивному стані — прозора з білою рамкою 50% (як "Custom" у макеті). Іконки — ті самі
 * Figma-гліфи, що на віджеті й екрані додавання (`categoryLineArtIconRes`), кастомні — Material.
 */
@Composable
private fun CategoryCard(
    item: CategoryTodaySummary,
    onToggleTimer: () -> Unit,
    onAddTime: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val isTracking = item.trackingStartTime != null
    val accentColor = categoryColor(item.category.colorHex)
    val displayName = categoryDisplayName(item.category)
    val shape = RoundedCornerShape(28.dp)

    val colorSpec = tween<Color>(TeperaMotion.LONG2, easing = TeperaMotion.Emphasized)
    val containerColor by animateColorAsState(
        targetValue = when {
            isTracking -> TeperaPalette.activityCardActive
            else -> TeperaPalette.activityCardIdle
        },
        animationSpec = colorSpec, label = "cardContainer"
    )
    val nameColor by animateColorAsState(if (isTracking) Color.White else Color.Black, colorSpec, label = "cardName")
    val badgeColor by animateColorAsState(
        if (isTracking) Color.White.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.2f), colorSpec, label = "cardBadge"
    )
    val glyphColor by animateColorAsState(if (isTracking) Color.White else accentColor, colorSpec, label = "cardGlyph")
    val nameSize by animateFloatAsState(
        targetValue = if (isTracking) 18f else 16f,
        animationSpec = tween(TeperaMotion.LONG2, easing = TeperaMotion.Emphasized), label = "cardNameSize"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            // Тінь Figma (0 16 20 @5%) — лише Android 10+: на Huawei P9 (Android 8) 6 елевейшн-тіней
            // на картках давали ~12 пунктів рваних кадрів прокрутки Home, а різниця майже непомітна (5%).
            .then(
                if (Build.VERSION.SDK_INT >= 29) {
                    Modifier.shadow(
                        8.dp, shape,
                        ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(containerColor)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.padding(4.dp).clickable(onClick = onOpenHistory),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                val lineArt = categoryLineArtIconRes(item.category.iconName)
                if (lineArt != null) {
                    Icon(painterResource(lineArt), contentDescription = null, tint = glyphColor, modifier = Modifier.size(16.dp))
                } else {
                    Icon(categoryIcon(item.category.iconName), contentDescription = null, tint = glyphColor, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = displayName,
                color = nameColor,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = nameSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TeperaIconButton(
                icon = if (isTracking) Icons.Filled.Pause else Icons.Outlined.PlayArrow,
                contentDescription = stringResource(
                    if (isTracking) R.string.category_stop_action else R.string.category_start_action
                ),
                onClick = onToggleTimer,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(32.dp)
            )
            AnimatedVisibility(
                visible = !isTracking,
                enter = fadeIn(tween(TeperaMotion.SHORT4, easing = TeperaMotion.EmphasizedDecelerate)) +
                    expandHorizontally(tween(TeperaMotion.LONG2, easing = TeperaMotion.Emphasized), expandFrom = Alignment.Start),
                exit = fadeOut(tween(TeperaMotion.SHORT3, easing = TeperaMotion.EmphasizedAccelerate)) +
                    shrinkHorizontally(tween(TeperaMotion.LONG2, easing = TeperaMotion.Emphasized), shrinkTowards = Alignment.Start)
            ) {
                Row {
                    Spacer(Modifier.width(4.dp))
                    TeperaIconButton(
                        icon = Icons.Filled.MoreTime,
                        contentDescription = stringResource(R.string.add_time_action_format, displayName),
                        onClick = onAddTime,
                        containerColor = TeperaPalette.activityMoreTime,
                        contentColor = TeperaPalette.buttonBrandDark
                    )
                }
            }
        }
    }
}
