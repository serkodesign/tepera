package com.serkodesign.tepera

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.serkodesign.tepera.widget.InfoActive2x1Receiver
import com.serkodesign.tepera.widget.InfoLast4x1Receiver
import com.serkodesign.tepera.widget.InfoOnlineBar4x1Receiver
import com.serkodesign.tepera.widget.InfoOnlineRing2x1Receiver
import com.serkodesign.tepera.widget.InfoOrb1x1Receiver
import com.serkodesign.tepera.widget.InfoStart2x1Receiver
import com.serkodesign.tepera.widget.InfoWeek4x1Receiver
import com.serkodesign.tepera.widget.TeperaWidget1x1Receiver
import com.serkodesign.tepera.widget.TeperaWidget2x1Receiver
import com.serkodesign.tepera.widget.TeperaWidget4x2Receiver
import com.serkodesign.tepera.widget.TeperaWidgetReceiver
import androidx.room.Room
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.WeeklySummaryWorker
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.local.DeviceIdProvider
import com.serkodesign.tepera.data.local.MIGRATION_1_2
import com.serkodesign.tepera.data.local.MIGRATION_2_3
import com.serkodesign.tepera.data.local.MIGRATION_3_4
import com.serkodesign.tepera.data.local.MIGRATION_4_5
import com.serkodesign.tepera.data.local.MIGRATION_5_6
import com.serkodesign.tepera.data.local.MIGRATION_6_7
import com.serkodesign.tepera.data.local.MIGRATION_7_8
import com.serkodesign.tepera.data.local.MIGRATION_8_9
import com.serkodesign.tepera.data.local.MIGRATION_9_10
import com.serkodesign.tepera.data.local.MIGRATION_10_11
import com.serkodesign.tepera.data.local.MIGRATION_11_12
import com.serkodesign.tepera.data.local.MIGRATION_12_13
import com.serkodesign.tepera.data.local.MIGRATION_13_14
import com.serkodesign.tepera.data.local.MIGRATION_14_15
import com.serkodesign.tepera.data.local.MIGRATION_15_16
import com.serkodesign.tepera.data.local.MIGRATION_16_17
import com.serkodesign.tepera.data.local.MIGRATION_17_18
import com.serkodesign.tepera.data.local.MIGRATION_18_19
import com.serkodesign.tepera.data.local.MIGRATION_19_20
import com.serkodesign.tepera.data.local.MIGRATION_20_21
import com.serkodesign.tepera.data.UsageSlotWorker
import com.serkodesign.tepera.data.repository.FeedAppRepository
import com.serkodesign.tepera.data.repository.RiskWindowRepository
import com.serkodesign.tepera.data.repository.RiskScanner
import com.serkodesign.tepera.data.repository.TesterStatsRepository
import com.serkodesign.tepera.data.repository.FeedSuggestionProvider
import com.serkodesign.tepera.data.NudgeChannel
import com.serkodesign.tepera.data.repository.ScanSnapshotStore
import com.serkodesign.tepera.data.RiskScanWorker
import com.serkodesign.tepera.data.repository.UsageSlotRepository
import com.serkodesign.tepera.data.repository.WelcomeBackRepository
import com.serkodesign.tepera.widget.DaySnapshotRepository
import com.serkodesign.tepera.widget.WeekSnapshotRepository
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BackupRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CardHistoryRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.DaySummaryRepository
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.GateEventRepository
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.data.repository.PatternRepository
import com.serkodesign.tepera.data.repository.ProAccess
import com.serkodesign.tepera.data.repository.FreeOnlyProAccess
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.RoomActivityRepository
import com.serkodesign.tepera.data.repository.RoomCategoryRepository
import com.serkodesign.tepera.data.repository.RoomExcludedAppRepository
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import com.serkodesign.tepera.widget.WidgetUpdateWorker
import com.serkodesign.tepera.widget.WidgetRolloverWorker
import com.serkodesign.tepera.util.CrashReporting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Без DI-фреймворку (CLAUDE.md) — ручний factory pattern. Усі залежності будуються тут
 * лениво й живуть на весь час життя процесу; ViewModel-и отримують готові Repository
 * через власні factory, ніколи не торкаючись AppDatabase напряму.
 */
class TeperaApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "tepera.db")
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21
            )
            .build()
    }

    val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(database.categoryDao())
    }

    val activityRepository: ActivityRepository by lazy {
        RoomActivityRepository(database.activityEntryDao())
    }

    val excludedAppRepository: ExcludedAppRepository by lazy {
        RoomExcludedAppRepository(database.excludedAppDao())
    }

    val balanceRepository: BalanceRepository by lazy {
        BalanceRepository(this, database.excludedAppDao())
    }

    val pauseRepository: PauseRepository by lazy {
        PauseRepository(this, database.detectedGapDao())
    }

    val patternRepository: PatternRepository by lazy {
        PatternRepository(this, database.excludedAppDao())
    }

    val sleepWindowRepository: SleepWindowRepository by lazy {
        SleepWindowRepository(database.sleepWindowDao())
    }

    val userEstimateRepository: UserEstimateRepository by lazy {
        UserEstimateRepository(database.userEstimateDao())
    }

    val unlockRepository: UnlockRepository by lazy { UnlockRepository(this) }

    // v3.0 MVP (docs/CLAUDE-CODE-PLAN-v3-MVP.md): стрічки, години ризику з планами, 15-хвилинні слоти часу.
    val feedAppRepository: FeedAppRepository by lazy { FeedAppRepository(this, database.feedAppDao()) }
    val riskWindowRepository: RiskWindowRepository by lazy {
        RiskWindowRepository(database.riskWindowDao(), database.planDao(), database.usageSlotDao())
    }
    val riskScanner: RiskScanner by lazy {
        RiskScanner(
            usageSlotRepository,
            riskWindowRepository,
            object : ScanSnapshotStore {
                override suspend fun load() = settingsStore.riskScanSnapshots()
                override suspend fun save(raw: String) = settingsStore.setRiskScanSnapshots(raw)
            },
            database.observationPeriodDao()
        )
    }
    val usageSlotRepository: UsageSlotRepository by lazy { UsageSlotRepository(this, database.usageSlotDao(), feedAppRepository, settingsStore) }
    val testerStatsRepository: TesterStatsRepository by lazy {
        TesterStatsRepository(
            usageSlotRepository, feedAppRepository, database.riskWindowDao(), database.planDao(),
            database.nudgeEventDao(), database.observationPeriodDao()
        )
    }
    val feedSuggestionProvider: FeedSuggestionProvider by lazy { FeedSuggestionProvider(this) }
    val nudgeEventDao get() = database.nudgeEventDao()

    // P-4 (CLAUDE-CODE-PLAN-pro.md): єдина точка доступу до стану Pro — поки без платіжного
    // шару (Ж-1), жодного споживача ще немає. Майбутні Pro-екрани (P-11…) читають лише звідси.
    val proAccess: ProAccess by lazy { FreeOnlyProAccess(this, settingsStore) }

    // P-2 (CLAUDE-CODE-PLAN-pro.md): щоденний Online-підсумок для майбутньої Pro-статистики
    // "Місяць"/"Рік" — викликається з WidgetUpdateWorker/WidgetRolloverWorker (D-39, варіант B+),
    // не читається жодним Free-екраном.
    val daySummaryRepository: DaySummaryRepository by lazy {
        DaySummaryRepository(
            database.daySummaryDao(),
            balanceRepository,
            unlockRepository,
            database.excludedAppDao(),
            sleepWindowRepository
        )
    }

    val gateRepository: GateRepository by lazy { GateRepository(this, database.appGateDao(), settingsStore, sleepWindowRepository) }

    val cardHistoryRepository: CardHistoryRepository by lazy {
        CardHistoryRepository(database.cardShowDao(), settingsStore)
    }

    val gateEventRepository: GateEventRepository by lazy { GateEventRepository(database.gateEventDao()) }

    val welcomeBackRepository: WelcomeBackRepository by lazy {
        WelcomeBackRepository(settingsStore, balanceRepository, activityRepository)
    }

    // W-1 (CLAUDE-CODE-TASK-widgets.md): спільна модель даних для нових віджетів — "Сьогодні"/
    // "Пульт" читають DaySnapshot, "Тиждень" — WeekSnapshot. Ще не має власного UI-споживача (W-3+).
    val daySnapshotRepository: DaySnapshotRepository by lazy {
        DaySnapshotRepository(balanceRepository, pauseRepository, activityRepository, sleepWindowRepository, settingsStore)
    }

    val weekSnapshotRepository: WeekSnapshotRepository by lazy {
        WeekSnapshotRepository(balanceRepository)
    }

    val deviceIdProvider: DeviceIdProvider by lazy { DeviceIdProvider(this) }

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    val activeTimerStore: ActiveTimerStore by lazy { ActiveTimerStore(this) }

    val installedAppsProvider: InstalledAppsProvider by lazy { InstalledAppsProvider(this) }

    val backupRepository: BackupRepository by lazy { BackupRepository(database, settingsStore) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // D-15: звіти про збої — за збереженим вибором користувача (маніфест за замовчуванням вимикає збір).
        CrashReporting.apply(this)
        // FR-2.1: insertDefaults() ігнорує вже засіяні рядки (fixed id + OnConflictStrategy.IGNORE
        // у CategoryDao), тож виклик щозапуску безпечний.
        applicationScope.launch { seedInitialData() }
        // FR-4.3: ~30 хв, KEEP — переживає перезапуск процесу, не дублюється щозапуску.
        WidgetUpdateWorker.schedule(this)
        WidgetRolloverWorker.schedule(this)
        // CC-8: канал єдиного сповіщення (тижневий підсумок) і планування, якщо людина його вмикала.
        WeeklySummaryWorker.createChannel(this)
        applicationScope.launch {
            if (settingsStore.weeklySummaryEnabled.first()) WeeklySummaryWorker.ensureScheduled(this@TeperaApp)
        }
        // D-25: щоденні фонові знімки скасовано; прибираємо роботу, яку могли запланувати ранні збірки.
        androidx.work.WorkManager.getInstance(this).cancelUniqueWork("daily_snapshot")
        // v3.0 MVP: збір слотів лише в beta-збірці; без списку стрічок воркер нічого не робить.
        if (BuildConfig.V3_MVP) {
            UsageSlotWorker.schedule(this)
            RiskScanWorker.schedule(this)
            NudgeChannel.create(this)
        }
        registerWidgetPreviewIfNeeded()
    }

    /** Початкові дані, безпечні до повторного виклику щозапуску (див. [onCreate]); після [wipeAllData] викликаються ще раз. */
    private suspend fun seedInitialData() {
        categoryRepository.ensureDefaultsSeeded(DefaultCategories.all)
        // SRS v2.4: "Сон" прибрано з дефолтних категорій — archive(), не видалення (FR-2.3).
        // Ідемпотентно (archive() лише виставляє isHidden=true), безпечно викликати щозапуску;
        // не чіпає вже існуючі записи цієї категорії, лише ховає її з активного списку.
        categoryRepository.archive(DefaultCategories.LEGACY_SLEEP_ID)
        // FR-D.9: окрема точка відліку для порогу готовності теплового патерну доби.
        settingsStore.seedFirstLaunchMillisIfUnset()
        // T-12: дефолт SRS "вікно сну (00:00-06:00)" — слот 1 ввімкнений, слот 2 (друге
        // вікно для плаваючого графіка) вимкнений, доки користувач не ввімкне сам.
        sleepWindowRepository.seedDefaultsIfUnset()
        // P-3 (CLAUDE-CODE-PLAN-pro.md): grandfathering — versionCode, на якому Tepera вперше
        // побачила цей код. runCatching: власний пакет теоретично завжди знаходиться, але це той
        // самий обережний стиль, що вже є для версії застосунку в AboutScreen.kt.
        runCatching {
            val info = packageManager.getPackageInfo(packageName, 0)
            settingsStore.seedFirstSeenIfUnset(PackageInfoCompat.getLongVersionCode(info).toInt())
        }
    }

    /**
     * Повне видалення всіх локальних даних (Резервне копіювання → Видалити всі дані, після двох підтверджень):
     * Room (усі таблиці), усі DataStore (налаштування й прапорці онбордингу, активні таймери, накопичені секунди,
     * id пристрою), мова застосунку, фонові завдання й сповіщення, закріплені ярлики воріт (вимикаються — Android
     * не дозволяє застосунку зняти закріплення), потім заново засіваються дефолтні категорії. Копію в Android Auto
     * Backup (якщо ввімкнена) це не чіпає — вона в акаунті Google. Після виклику треба перезапустити застосунок
     * ([restartApp]), щоб усі екрани й кеші стартували з чистого стану.
     */
    suspend fun wipeAllData() = withContext(Dispatchers.IO) {
        androidx.work.WorkManager.getInstance(this@TeperaApp).cancelAllWork()
        runCatching {
            getSystemService(android.app.NotificationManager::class.java)?.cancelAll()
            val shortcuts = getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcuts != null) {
                val pinnedIds = shortcuts.pinnedShortcuts.map { it.id }
                if (pinnedIds.isNotEmpty()) shortcuts.disableShortcuts(pinnedIds)
                shortcuts.removeAllDynamicShortcuts()
            }
        }
        database.clearAllTables()
        settingsStore.clearAll()
        activeTimerStore.clearAll()
        deviceIdProvider.clearAll()
        getSharedPreferences("locale_prefs", MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences("widget_prefs", MODE_PRIVATE).edit().clear().commit()
        CrashReporting.reset(this@TeperaApp)
        seedInitialData()
        WidgetUpdateWorker.schedule(this@TeperaApp)
        WidgetRolloverWorker.schedule(this@TeperaApp)
        runCatching { activeTimerStore.refreshWidgets() }
    }

    /** Перезапуск із чистого стану: нова задача з головною активністю й завершення процесу (кеші DataStore/Room скидаються). */
    fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    /**
     * Генероване прев'ю віджета для меню віджетів (Android 15+/API 35, Glance 1.2.0
     * `setWidgetPreviews` → `TeperaWidget.providePreview`). **Система скидає згенеровані прев'ю при
     * оновленні застосунку**, тож ключ реєстрації — [WIDGET_PREVIEW_VERSION] + час останнього
     * оновлення пакета (`lastUpdateTime`), а не лише версія. Виклик обмежений системою за частотою
     * (кілька на годину) і при перевищенні повертає ненульовий результат замість помилки — тому успіх запам'ятовується
     * ОКРЕМО для кожного провайдера (4x1/4x2), а невдалий повторюється при наступному запуску.
     * На Android < 15 діє статичний `previewImage`.
     */
    private fun registerWidgetPreviewIfNeeded() {
        if (Build.VERSION.SDK_INT < 35) return
        val lastUpdate = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        val key = "$WIDGET_PREVIEW_VERSION:$lastUpdate"
        val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
        applicationScope.launch {
            val manager = GlanceAppWidgetManager(this@TeperaApp)
            val receivers = listOf(
                "preview_key_4x1" to TeperaWidgetReceiver::class,
                "preview_key_4x2" to TeperaWidget4x2Receiver::class,
                "preview_key_1x1" to TeperaWidget1x1Receiver::class,
                "preview_key_2x1" to TeperaWidget2x1Receiver::class,
                "preview_key_info_online_ring" to InfoOnlineRing2x1Receiver::class,
                "preview_key_info_online_bar" to InfoOnlineBar4x1Receiver::class,
                "preview_key_info_active" to InfoActive2x1Receiver::class,
                "preview_key_info_start" to InfoStart2x1Receiver::class,
                "preview_key_info_orb" to InfoOrb1x1Receiver::class,
                "preview_key_info_last" to InfoLast4x1Receiver::class,
                "preview_key_info_week" to InfoWeek4x1Receiver::class
            )
            for ((prefKey, receiver) in receivers) {
                if (prefs.getString(prefKey, null) == key) continue
                runCatching { manager.setWidgetPreviews(receiver) }
                    .onSuccess { result ->
                        Log.i("WidgetPreview", "${receiver.simpleName}=$result")
                        // Результат — кількість ВІДХИЛЕНИХ викликів (rate-limited): 0 = успіх, > 0 = ліміт частоти
                        // (Glance пише "setWidgetPreview call ... was rate-limited") — повторимо при наступному запуску.
                        if (result.toString() == "0") prefs.edit().putString(prefKey, key).apply()
                    }
                    .onFailure { Log.w("WidgetPreview", "setWidgetPreviews failed", it) }
            }
        }
    }

    private companion object {
        const val WIDGET_PREVIEW_VERSION = 5
    }
}
