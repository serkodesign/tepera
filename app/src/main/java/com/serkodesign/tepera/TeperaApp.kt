package com.serkodesign.tepera

import android.app.Application
import androidx.room.Room
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.local.DeviceIdProvider
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.BalanceRepository
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.data.repository.ExcludedAppRepository
import com.serkodesign.tepera.data.repository.InstalledAppsProvider
import com.serkodesign.tepera.data.repository.RoomActivityRepository
import com.serkodesign.tepera.data.repository.RoomCategoryRepository
import com.serkodesign.tepera.data.repository.RoomExcludedAppRepository
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
        Room.databaseBuilder(this, AppDatabase::class.java, "tepera.db").build()
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

    val deviceIdProvider: DeviceIdProvider by lazy { DeviceIdProvider(this) }

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    val installedAppsProvider: InstalledAppsProvider by lazy { InstalledAppsProvider(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // FR-2.1: insertDefaults() ігнорує вже засіяні рядки (fixed id + OnConflictStrategy.IGNORE
        // у CategoryDao), тож виклик щозапуску безпечний.
        applicationScope.launch {
            categoryRepository.ensureDefaultsSeeded(DefaultCategories.all)
        }
    }
}
