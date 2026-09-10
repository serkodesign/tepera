package com.serkodesign.tepera.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.serkodesign.tepera.data.local.dao.ActivityEntryDao
import com.serkodesign.tepera.data.local.dao.CategoryDao
import com.serkodesign.tepera.data.local.dao.DetectedGapDao
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity
import com.serkodesign.tepera.data.local.entity.ExcludedAppEntity

/**
 * NFR-5.3: явна стратегія міграцій з першої версії — НІКОЛИ не fallbackToDestructiveMigration().
 * Коли з'являється зміна схеми, версія тут ОБОВ'ЯЗКОВО зростає, і додається Migration-об'єкт
 * (`Migrations.kt`) у `Room.databaseBuilder(...).addMigrations(...)` при білді інстансу
 * (TeperaApp.kt).
 *
 * **Урок, підтверджений на реальному пристрої (Samsung S23):** DetectedGapEntity спершу додали
 * прямо у версію 1 без bump — застосунок ще не опублікований, тож здавалось, що "реальних
 * установок з даними" нема й перевіряти нема на чому. Хибне припущення: тестові пристрої ВЖЕ
 * мають встановлений застосунок зі старою схемою. Room звіряє identity hash схеми при відкритті
 * БД — новий (4-таблична v1) не збігся зі старим (3-таблична v1) на пристрої, `IllegalStateException`
 * ("Room cannot verify the data integrity... changed schema but forgot to update the version
 * number") одразу при старті, застосунок падав циклічно (`adb install -r` зберігає дані застосунку,
 * DB-файл лишається старим). Тому: bump версії — ЗАВЖДИ, щойно змінюється список entities/полів,
 * навіть у ще неопублікованому застосунку.
 *
 * v2.0: додано ExcludedAppEntity (FR-3.5, Exclusion List для Online-часу) — у версію 1 (до
 * встановлення на тестові пристрої, тому без наслідків).
 * v2.6: додано DetectedGapEntity (FR-D.1/D.5 — виявлені паузи й їх позначення) — версія 2,
 * `MIGRATION_1_2` (`Migrations.kt`).
 */
@Database(
    entities = [
        CategoryEntity::class,
        ActivityEntryEntity::class,
        ExcludedAppEntity::class,
        DetectedGapEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun activityEntryDao(): ActivityEntryDao
    abstract fun excludedAppDao(): ExcludedAppDao
    abstract fun detectedGapDao(): DetectedGapDao
}
