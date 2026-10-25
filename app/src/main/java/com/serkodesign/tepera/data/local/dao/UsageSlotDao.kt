package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.serkodesign.tepera.data.local.entity.UsageSlotEntity

/** v3.0 MVP, Етап 0/1. Доба перезаписується цілком: слоти однієї дати завжди приходять з одного підрахунку. */
@Dao
interface UsageSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<UsageSlotEntity>)

    @Query("DELETE FROM usage_slots WHERE date = :epochDay")
    suspend fun deleteDay(epochDay: Long)

    @Transaction
    suspend fun replaceDay(epochDay: Long, slots: List<UsageSlotEntity>) {
        deleteDay(epochDay)
        insertAll(slots)
    }

    @Query("SELECT * FROM usage_slots WHERE date BETWEEN :fromEpochDay AND :toEpochDay")
    suspend fun getRange(fromEpochDay: Long, toEpochDay: Long): List<UsageSlotEntity>

    @Query("SELECT DISTINCT packageName FROM usage_slots")
    suspend fun getPackages(): List<String>

    @Query("SELECT COUNT(DISTINCT date) FROM usage_slots")
    suspend fun countDaysWithRows(): Int

    @Query("DELETE FROM usage_slots WHERE date < :beforeEpochDay")
    suspend fun deleteOlderThan(beforeEpochDay: Long)
}
