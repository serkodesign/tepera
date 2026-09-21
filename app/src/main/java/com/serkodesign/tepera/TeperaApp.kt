package com.serkodesign.tepera

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.serkodesign.tepera.widget.TeperaWidget1x1Receiver
import com.serkodesign.tepera.widget.TeperaWidget2x1Receiver
import com.serkodesign.tepera.widget.TeperaWidget4x2Receiver
import com.serkodesign.tepera.widget.TeperaWidgetReceiver
import androidx.room.Room
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.ActiveTimerStore
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
import com.serkodesign.tepera.data.repository.RoomActivityRepository
import com.serkodesign.tepera.data.repository.RoomCategoryRepository
import com.serkodesign.tepera.data.repository.RoomExcludedAppRepository
import com.serkodesign.tepera.data.createTimerCheckNotificationChannel
import com.serkodesign.tepera.data.repository.SleepWindowRepository
import com.serkodesign.tepera.data.repository.UnlockRepository
import com.serkodesign.tepera.data.repository.UserEstimateRepository
import com.serkodesign.tepera.widget.WidgetUpdateWorker
import com.serkodesign.tepera.widget.WidgetRolloverWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9
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

    val gateRepository: GateRepository by lazy { GateRepository(this, database.appGateDao(), settingsStore) }

    val cardHistoryRepository: CardHistoryRepository by lazy {
        CardHistoryRepository(database.cardShowDao(), settingsStore)
    }

    val gateEventRepository: GateEventRepository by lazy { GateEventRepository(database.gateEventDao()) }

    val deviceIdProvider: DeviceIdProvider by lazy { DeviceIdProvider(this) }

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    val activeTimerStore: ActiveTimerStore by lazy { ActiveTimerStore(this) }

    val installedAppsProvider: InstalledAppsProvider by lazy { InstalledAppsProvider(this) }

    val backupRepository: BackupRepository by lazy { BackupRepository(database, settingsStore) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // FR-2.1: insertDefaults() ігнорує вже засіяні рядки (fixed id + OnConflictStrategy.IGNORE
        // у CategoryDao), тож виклик щозапуску безпечний.
        applicationScope.launch { seedInitialData() }
        // FR-4.3: ~30 хв, KEEP — переживає перезапуск процесу, не дублюється щозапуску.
        WidgetUpdateWorker.schedule(this)
        WidgetRolloverWorker.schedule(this)
        // Сповіщення "усе ще цим займаєшся?" (TimerCheckWorker) — createNotificationChannel()
        // ідемпотентний, безпечно викликати щозапуску.
        createTimerCheckNotificationChannel(this)
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
                "preview_key_2x1" to TeperaWidget2x1Receiver::class
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
        const val WIDGET_PREVIEW_VERSION = 4
    }
}
