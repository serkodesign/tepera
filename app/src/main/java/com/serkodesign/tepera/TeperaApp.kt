package com.serkodesign.tepera

import android.app.Application
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
        applicationScope.launch {
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
        // FR-4.3: ~30 хв, KEEP — переживає перезапуск процесу, не дублюється щозапуску.
        WidgetUpdateWorker.schedule(this)
        // Сповіщення "усе ще цим займаєшся?" (TimerCheckWorker) — createNotificationChannel()
        // ідемпотентний, безпечно викликати щозапуску.
        createTimerCheckNotificationChannel(this)
        registerWidgetPreviewIfNeeded()
    }

    /**
     * Генероване прев'ю віджета для меню віджетів (Android 15+/API 35, Glance 1.2.0
     * `setWidgetPreviews` → `TeperaWidget.providePreview`). Системний виклик обмежений за частотою,
     * тому реєструється лише коли змінилась [WIDGET_PREVIEW_VERSION] (піднімати, коли змінюється
     * вигляд віджета), а не щозапуску. На Android < 15 діє статичний `previewImage`.
     */
    private fun registerWidgetPreviewIfNeeded() {
        if (Build.VERSION.SDK_INT < 35) return
        val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
        if (prefs.getInt("preview_version", 0) >= WIDGET_PREVIEW_VERSION) return
        applicationScope.launch {
            runCatching {
                GlanceAppWidgetManager(this@TeperaApp).setWidgetPreviews(TeperaWidgetReceiver::class)
            }.onSuccess {
                prefs.edit().putInt("preview_version", WIDGET_PREVIEW_VERSION).apply()
            }
        }
    }

    private companion object {
        const val WIDGET_PREVIEW_VERSION = 2
    }
}
