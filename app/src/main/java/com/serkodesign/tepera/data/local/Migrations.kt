package com.serkodesign.tepera.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: додано `detected_gaps` (DetectedGapEntity, FR-D.1/D.5, SRS v2.6) — виявлені паузи
 * без активності на передньому плані й їхнє позначення категорією. SQL звірений з тим, що
 * генерує Room для поточного визначення entity (`app/schemas/.../2.json`), НЕ писати навмання —
 * розбіжність з реальною схемою Room згенерує при першому відкритті БД на пристрої з версією 1
 * (підтверджено на Samsung S23 у цій самій сесії).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `detected_gaps` (" +
                "`id` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, " +
                "`durationMinutes` INTEGER NOT NULL, " +
                "`labeledEntryId` TEXT, " +
                "`dismissed` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_detected_gaps_startTime` " +
                "ON `detected_gaps` (`startTime`)"
        )
    }
}

/**
 * v2 → v3: додано `sleep_windows` (SleepWindowEntity, T-12 tepera-dev-spec.md) — до 2 слотів
 * вікна сну як технічний параметр розрахунку. Таблиця порожня одразу після міграції;
 * `SleepWindowRepository.seedDefaultsIfUnset()` (TeperaApp.onCreate()) засіює дефолти при
 * наступному запуску, той самий підхід, що й `ensureDefaultsSeeded()` для категорій.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_windows` (" +
                "`slot` INTEGER NOT NULL, " +
                "`startMinuteOfDay` INTEGER NOT NULL, " +
                "`endMinuteOfDay` INTEGER NOT NULL, " +
                "`enabled` INTEGER NOT NULL, " +
                "PRIMARY KEY(`slot`))"
        )
    }
}

/**
 * v3 → v4: додано `user_estimates` (UserEstimateEntity, T-3 tepera-dev-spec.md, розділ 2.2
 * "принцип пасивного сорому") — оцінка користувача + пізніше дораховане реальне значення.
 * `forDate` (java.time.LocalDate) зберігається як epoch-day (`Converters.kt`), тому в SQL має
 * тип INTEGER, не TEXT. SQL звірений з `app/schemas/.../4.json`, як і попередні міграції.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_estimates` (" +
                "`id` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`estimatedValue` INTEGER NOT NULL, " +
                "`actualValue` INTEGER, " +
                "`forDate` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

/**
 * v4 → v5: додано `app_gates` (AppGateEntity, T-4 tepera-dev-spec.md, FR-G частина 1) — застосунки
 * з паузою перед запуском, обрані користувачем. `packageName` — первинний ключ (не UUID), один
 * рядок на застосунок. SQL звірений з `app/schemas/.../5.json`, як і попередні міграції.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_gates` (" +
                "`packageName` TEXT NOT NULL, " +
                "`delaySeconds` INTEGER NOT NULL, " +
                "`originalIconHandled` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`packageName`))"
        )
    }
}

/**
 * v5 → v6: додано `lastProceedAtMillis` до `app_gates` (T-5 tepera-dev-spec.md) — момент
 * останнього успішного проходження паузи, потрібен для дебаунсу "повторний тап протягом 30 с
 * не показує паузу вдруге". `DEFAULT 0` — існуючі рядки трактуються як "ще ніколи не проходили".
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_gates ADD COLUMN lastProceedAtMillis INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * v6 → v7: додано `card_show_history` (CardShowEntity, T-13 tepera-dev-spec.md "рушій карток") —
 * єдина історія показів контекстних карток, замінює розкидані по окремих ViewModel `SettingsStore`
 * ключі (`lastReflectionHandledAtMillis`/`lastUnlockEstimateHandledMillis`/
 * `lastPhoneUseEstimateHandledMillis`, усі видалені цим тікетом). Старі значення цих ключів
 * НЕ переносяться — застосунок ще не опублікований (CLAUDE.md), а найгірший практичний наслідок
 * "одна картка-оцінка може показатись на день-два раніше очікуваного" одразу після цього
 * оновлення, не критично.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `card_show_history` (" +
                "`id` TEXT NOT NULL, " +
                "`cardType` TEXT NOT NULL, " +
                "`isEstimate` INTEGER NOT NULL, " +
                "`atMillis` INTEGER NOT NULL, " +
                "`result` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

/**
 * v7 → v8: додано `gate_events` (GateEventEntity, T-6 tepera-dev-spec.md "події воріт і
 * свідчення спроможності") — по одному запису на кожне реальне розв'язання екрана паузи воріт
 * (T-5), годує картку FR-P.3.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `gate_events` (" +
                "`id` TEXT NOT NULL, " +
                "`packageName` TEXT NOT NULL, " +
                "`result` TEXT NOT NULL, " +
                "`atMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

/**
 * v8 → v9: додано `shortcutId` до AppGateEntity — реальний баг, знайдений користувачем на
 * Samsung S23: старий детермінований ID (`"gate_" + packageName`, обчислювався на льоту, ніде
 * не зберігався) означав, що ПОВТОРНЕ створення воріт для того самого застосунку (після
 * видалення через "Прибрати") завжди намагалося перевикористати ТОЙ САМИЙ ID ярлика. Android
 * не дає апці по-справжньому видалити закріплений ярлик — `removeGate()` лише вимикає його
 * (`disableShortcuts()`), тож той ID і далі "існує" в системі. `GateRepository.createGate()`
 * бачив це й ішов гілкою "увімкнути наявний вимкнений ярлик" (`enableShortcuts()`) ЗАМІСТЬ
 * `requestPinShortcut()` — а `enableShortcuts()` не показує жодного системного UI розміщення на
 * робочому столі, просто знімає прапорець "вимкнено" з запису, який лишається де він і був
 * (іноді взагалі не на видимому лаунчері — див. нижче). Наслідок: застосунок рапортував "Ярлик
 * створено", а іконка ніде не з'являлась. Фікс — кожне створення воріт тепер генерує СВІЖИЙ
 * унікальний `shortcutId` (`"gate_" + packageName + "_" + System.currentTimeMillis()`), тож
 * `requestPinShortcut()` завжди бачить абсолютно новий ID і System ЗАВЖДИ показує справжній
 * діалог розміщення — стара "enableShortcuts() для вимкненого ID" гілка в коді прибрана
 * повністю, вона більше не потрібна (і не може кинути той самий `IllegalArgumentException`,
 * що вона раніше запобігала, бо ID більше ніколи не повторюється).
 * Існуючі рядки отримують `shortcutId = "gate_" + packageName` (той самий детермінований
 * розрахунок, що раніше) — зберігає сумісність із уже закріпленими ярликами, які насправді
 * коректно видимі (напр. якщо пощастило з першою спробою).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_gates ADD COLUMN shortcutId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE app_gates SET shortcutId = 'gate_' || packageName WHERE shortcutId = ''")
    }
}

/**
 * Версія 10: активність, що триває через кілька діб, зберігається кількома записами (по одному на
 * логічну добу, див. `splitAtDayRollover`), пов'язаними спільним `seriesId`. NULL — звичайний
 * запис, старі рядки лишаються як були.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activity_entries ADD COLUMN seriesId TEXT")
    }
}

/** Версія 11 (CC-9): `metric_events` — локальні метрики використання, лише тип і час. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `metric_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_metric_events_timestamp` ON `metric_events` (`timestamp`)")
    }
}

/** Версія 12 (CC-4): `daily_snapshots` — щоденний знімок Online-хвилин. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `daily_snapshots` (" +
                "`dayEpoch` INTEGER NOT NULL, " +
                "`onlineMinutes` INTEGER NOT NULL, " +
                "`capturedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`dayEpoch`))"
        )
    }
}

/** Версія 13: прибрано `daily_snapshots` (додана у версії 12) — щоденні фонові знімки скасовано (D-25). */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `daily_snapshots`")
    }
}

/** Версія 14: прибрано `metric_events` (додана у версії 11) — локальні метрики скасовано (D-27). */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `metric_events`")
        db.execSQL("DELETE FROM gate_events WHERE result = 'PASSED_THROUGH'") // результат існував лише для скану обходів
    }
}

/** Версія 15 (CC-6): лічильник повторних відкриттів воріт для опційної «зростаючої» затримки. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_gates ADD COLUMN repeatCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE app_gates ADD COLUMN lastShownAtMillis INTEGER NOT NULL DEFAULT 0")
    }
}
