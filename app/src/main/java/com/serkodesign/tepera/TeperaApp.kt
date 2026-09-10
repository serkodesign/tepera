package com.serkodesign.tepera

import android.app.Application
import androidx.room.Room
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.ActiveTimerStore
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.local.DeviceIdProvider
import com.serkodesign.tepera.data.local.MIGRATION_1_2
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BackupRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.data.repository.PatternRepository
import com.serkodesign.tepera.data.repository.PauseRepository
import com.serkodesign.tepera.data.repository.RoomActivityRepository
import com.serkodesign.tepera.data.repository.RoomCategoryRepository
import com.serkodesign.tepera.data.repository.RoomExcludedAppRepository
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
            .addMigrations(MIGRATION_1_2)
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
            // FR-P.1: точка відліку тижневої рефлексії — перший запуск, не "0/ніколи" (інакше
            // картка з'явилась би одразу, коли ще нема тижня даних для порівняння).
            settingsStore.seedLastReflectionHandledAtIfUnset()
            // FR-D.9: окрема точка відліку для порогу готовності теплового патерну доби.
            settingsStore.seedFirstLaunchMillisIfUnset()
        }
        // FR-4.3: ~30 хв, KEEP — переживає перезапуск процесу, не дублюється щозапуску.
        WidgetUpdateWorker.schedule(this)
    }
}
