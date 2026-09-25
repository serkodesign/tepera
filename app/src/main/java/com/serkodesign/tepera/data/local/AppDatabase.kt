package com.serkodesign.tepera.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.serkodesign.tepera.data.local.dao.ActivityEntryDao
import com.serkodesign.tepera.data.local.dao.AppGateDao
import com.serkodesign.tepera.data.local.dao.CardShowDao
import com.serkodesign.tepera.data.local.dao.CategoryDao
import com.serkodesign.tepera.data.local.dao.DetectedGapDao
import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import com.serkodesign.tepera.data.local.dao.GateEventDao
import com.serkodesign.tepera.data.local.dao.SleepWindowDao
import com.serkodesign.tepera.data.local.dao.UserEstimateDao
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.AppGateEntity
import com.serkodesign.tepera.data.local.entity.CardShowEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity
import com.serkodesign.tepera.data.local.entity.ExcludedAppEntity
import com.serkodesign.tepera.data.local.entity.GateEventEntity
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import com.serkodesign.tepera.data.local.entity.UserEstimateEntity

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
 * T-12 (tepera-dev-spec.md): додано SleepWindowEntity (вікно сну як технічний параметр
 * розрахунку, до 2 слотів) — версія 3, `MIGRATION_2_3` (`Migrations.kt`).
 * T-3 (tepera-dev-spec.md): додано UserEstimateEntity (принцип пасивного сорому — оцінка
 * користувача поруч із реальним числом, замість пасивного показу) — версія 4, `MIGRATION_3_4`
 * (`Migrations.kt`).
 * Активність через кілька діб — кілька записів зі спільним `seriesId` (`activity_entries.seriesId`) — версія 10,
 * `MIGRATION_9_10` (`Migrations.kt`).
 * T-4 (tepera-dev-spec.md): додано AppGateEntity (FR-G частина 1 — застосунки з паузою перед
 * запуском) — версія 5, `MIGRATION_4_5` (`Migrations.kt`).
 * T-5 (tepera-dev-spec.md): додано `lastProceedAtMillis` до AppGateEntity (FR-G частина 2 —
 * дебаунс повторного тапу) — версія 6, `MIGRATION_5_6` (`Migrations.kt`).
 * T-13 (tepera-dev-spec.md): додано CardShowEntity (рушій карток — єдина історія показів,
 * замінює розкидані по ViewModel `SettingsStore`-ключі) — версія 7, `MIGRATION_6_7`
 * (`Migrations.kt`).
 * T-6 (tepera-dev-spec.md): додано GateEventEntity (події воріт — годує картку FR-P.3
 * "свідчення спроможності") — версія 8, `MIGRATION_7_8` (`Migrations.kt`).
 * Реальний баг, знайдений користувачем на Samsung S23: додано `shortcutId` до AppGateEntity —
 * ID закріпленого ярлика тепер зберігається в рядку замість обчислення наживо з packageName,
 * інакше повторне створення воріт для того самого застосунку мовчки "успішно" перевикористовувало
 * старий вимкнений ярлик без показу системного діалогу розміщення — версія 9, `MIGRATION_8_9`
 * (`Migrations.kt`).
 * Активність через кілька діб — кілька записів зі спільним `seriesId` (`activity_entries.seriesId`,
 * див. `splitAtDayRollover`) — версія 10, `MIGRATION_9_10` (`Migrations.kt`).
 * CC-9 (план закритого тесту): додано MetricEventEntity (локальні метрики — лише тип і час) —
 * версія 11, `MIGRATION_10_11` (`Migrations.kt`).
 * CC-4: у версії 12 була додана `daily_snapshots` (щоденний знімок Online) — за рішенням власника фонові знімки
 * скасовано, версія 13 (`MIGRATION_12_13`) прибирає таблицю. `MIGRATION_11_12` лишається для пристроїв на версії 11.
 * D-27: локальні метрики (`metric_events`, версія 11) скасовано — версія 14 (`MIGRATION_13_14`) прибирає таблицю.
 * CC-6: додано `repeatCount`/`lastShownAtMillis` до AppGateEntity (зростаюча затримка воріт) — версія 15, `MIGRATION_14_15`.
 */
@Database(
    entities = [
        CategoryEntity::class,
        ActivityEntryEntity::class,
        ExcludedAppEntity::class,
        DetectedGapEntity::class,
        SleepWindowEntity::class,
        UserEstimateEntity::class,
        AppGateEntity::class,
        CardShowEntity::class,
        GateEventEntity::class
    ],
    version = 15,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun activityEntryDao(): ActivityEntryDao
    abstract fun excludedAppDao(): ExcludedAppDao
    abstract fun detectedGapDao(): DetectedGapDao
    abstract fun sleepWindowDao(): SleepWindowDao
    abstract fun userEstimateDao(): UserEstimateDao
    abstract fun appGateDao(): AppGateDao
    abstract fun cardShowDao(): CardShowDao
    abstract fun gateEventDao(): GateEventDao
}
