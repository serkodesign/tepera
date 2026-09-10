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
