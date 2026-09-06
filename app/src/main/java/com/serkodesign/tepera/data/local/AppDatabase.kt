package com.serkodesign.tepera.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.serkodesign.tepera.data.local.dao.ActivityEntryDao
import com.serkodesign.tepera.data.local.dao.CategoryDao
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.local.entity.ExcludedAppEntity

/**
 * NFR-5.3: явна стратегія міграцій з першої версії — НІКОЛИ не fallbackToDestructiveMigration().
 * Коли з'явиться зміна схеми, додати Migration-об'єкт у Room.databaseBuilder(...).addMigrations(...)
 * при білді інстансу, а не тут — цей файл лишається декларацією схеми.
 *
 * v2.0: додано ExcludedAppEntity (FR-3.5, Exclusion List для Online-часу).
 */
@Database(
    entities = [CategoryEntity::class, ActivityEntryEntity::class, ExcludedAppEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun activityEntryDao(): ActivityEntryDao
    abstract fun excludedAppDao(): ExcludedAppDao
}
