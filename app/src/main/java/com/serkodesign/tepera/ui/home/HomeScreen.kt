package com.serkodesign.tepera.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.serkodesign.tepera.ui.pattern.PatternViewModel
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
    onAddEntryForCategory: (String) -> Unit,
    onShowOnboarding: () -> Unit,
    onShowValuesOnboarding: () -> Unit,
    onShowCategoryOnboarding: () -> Unit,
    onShowOnlineEstimateOnboarding: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(categoryRepository, activityRepository, activeTimerStore)
    )
    val summary by viewModel.todaySummary.collectAsState()

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
        factory = PatternViewModel.Factory(patternRepository, balanceRepository, settingsStore)
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
        lastPhoneUseEstimateState, weeklyDigestState, patternState, gateEventsSummaryState
    ) {
        cardStackViewModel.evaluate(
            listOf(
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

    // FR-P.2: питання про цінності ЗАВЖДИ показується першим при першому запуску — окремий
    // ефект, що не залежить від стану доступу до статистики.
    val valuesOnboardingSeen by settingsStore.valuesOnboardingSeen.collectAsState(initial = true)
    LaunchedEffect(valuesOnboardingSeen) {
        if (!valuesOnboardingSeen) {
            onShowValuesOnboarding()
        }
    }

    // T-8 (tepera-dev-spec.md), крок 2 "Порядку першого запуску": одразу після питання про
    // цінності, перед онбординг-оцінкою Online-часу — логічне продовження "що ти цінуєш" у
    // "що саме відмічатимеш" (документ: попередні 5 категорій самі по собі норма, FR-P.5).
    val categoryOnboardingSeen by settingsStore.categoryOnboardingSeen.collectAsState(initial = true)
    LaunchedEffect(valuesOnboardingSeen, categoryOnboardingSeen) {
        if (valuesOnboardingSeen && !categoryOnboardingSeen) {
            onShowCategoryOnboarding()
        }
    }

    // T-3, крок 3 "Порядку першого запуску": одразу після вибору категорій (T-8), ще до
    // пояснення дозволу нижче — не залежить від стану доступу до статистики, той самий принцип.
    val onlineEstimateOnboardingSeen by settingsStore.onlineEstimateOnboardingSeen.collectAsState(initial = true)
    LaunchedEffect(valuesOnboardingSeen, categoryOnboardingSeen, onlineEstimateOnboardingSeen) {
        if (valuesOnboardingSeen && categoryOnboardingSeen && !onlineEstimateOnboardingSeen) {
            onShowOnlineEstimateOnboarding()
        }
    }

    // FR-7.1: онбординг доступу до статистики — лише ПІСЛЯ того, як питання про цінності, вибір
    // категорій (T-8) і онбординг-оцінка Online-часу (T-3) вже показані, інакше кілька ефектів
    // могли б спробувати навігувати одночасно на першому запуску.
    val onboardingSeen by settingsStore.onboardingUsageAccessSeen.collectAsState(initial = true)
    LaunchedEffect(balanceState.hasUsageAccess, onboardingSeen, valuesOnboardingSeen, categoryOnboardingSeen, onlineEstimateOnboardingSeen) {
        if (valuesOnboardingSeen && categoryOnboardingSeen && onlineEstimateOnboardingSeen && balanceState.hasUsageAccess == false && !onboardingSeen) {
            onShowOnboarding()
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
            HomeHeader(onOpenSettings = onOpenSettings)

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

            // FR-D.10/D.10a/D.11: вертикальний стек до 3 контекстних карток, пріоритизований за
            // актуальністю — над карткою "Мій день", кожна сама вирішує, чи їй бути видимою.
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
                digestState = weeklyDigestState,
                onDismissDigest = weeklyDigestViewModel::dismiss,
                patternState = patternState,
                onDismissPattern = patternViewModel::dismiss,
                gateEventsSummaryState = gateEventsSummaryState,
                onDismissGateEventsSummary = gateEventsSummaryViewModel::dismiss
            )

            // "My day" (SRS v2.5, розділ 4.4) — ОДНА картка-обгортка (заголовок+шкала+легенда
            // разом), а не окремий заголовок над секцією без фону, як було раніше.
            // `vertical = 8.dp` на зовнішньому паддінгу — без нього відступ до сусідніх блоків
            // (контекстні картки зверху, заголовок "Активності" знизу) виходив 8dp замість
            // однакового 16dp ритму, яким рознесені решта секцій Home (кожна з них додає власні
            // 8dp зверху й знизу, тут бракувало пари).
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryCard(
                                        item = item,
                                        onToggleTimer = { viewModel.toggleTimer(item.category.id) },
                                        onAddTime = { onAddEntryForCategory(item.category.id) }
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
                fontWeight = FontWeight.Medium,
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
