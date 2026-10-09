package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.DaySummaryEntity

/** P-2 (`CLAUDE-CODE-PLAN-pro.md`). `insert()` ігнорує конфлікт — рядок, щойно записаний, більше не чіпається. */
@Dao
interface DaySummaryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(summary: DaySummaryEntity)

    /** Лише для імпорту резервної копії (FR-6.2) — таблиця перед цим очищена, REPLACE тут не ризикує "оживити" чужий рядок. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summaries: List<DaySummaryEntity>)

    /** Epoch-дні вже записаних діб — для вирахування кандидатів на дозапис ([com.serkodesign.tepera.util.DaySummaryBackfill]). */
    @Query("SELECT date FROM day_summaries")
    suspend fun getAllDates(): List<Long>

    @Query("SELECT * FROM day_summaries WHERE date BETWEEN :fromEpochDay AND :toEpochDay ORDER BY date")
    suspend fun getRange(fromEpochDay: Long, toEpochDay: Long): List<DaySummaryEntity>

    /** Резервне копіювання (FR-6.2). */
    @Query("SELECT * FROM day_summaries ORDER BY date")
    suspend fun getAllOnce(): List<DaySummaryEntity>

    @Query("DELETE FROM day_summaries")
    suspend fun deleteAll()
}
