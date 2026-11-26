package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.serkodesign.tepera.data.local.entity.DailyEventCounterEntity
import java.time.LocalDate

@Dao
interface DailyEventCounterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(row: DailyEventCounterEntity): Long

    @Query("UPDATE daily_event_counters SET count = count + 1 WHERE date = :date AND eventName = :eventName AND propsKey = :propsKey")
    suspend fun bump(date: LocalDate, eventName: String, propsKey: String): Int

    /** Upsert без `ON CONFLICT DO UPDATE` (потребує SQLite 3.24 / API 30, а minSdk = 26). */
    @Transaction
    suspend fun increment(date: LocalDate, eventName: String, propsKey: String) {
        if (insertIfAbsent(DailyEventCounterEntity(date, eventName, propsKey, 1)) == -1L) {
            bump(date, eventName, propsKey)
        }
    }

    @Query("SELECT * FROM daily_event_counters WHERE date >= :from AND date <= :to ORDER BY date, eventName, propsKey")
    suspend fun getRange(from: LocalDate, to: LocalDate): List<DailyEventCounterEntity>

    @Query("DELETE FROM daily_event_counters WHERE date < :before")
    suspend fun deleteOlderThan(before: LocalDate): Int

    @Query("DELETE FROM daily_event_counters")
    suspend fun deleteAll()
}
