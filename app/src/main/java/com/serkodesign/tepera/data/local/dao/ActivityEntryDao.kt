package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEntryDao {

    @Query("SELECT * FROM activity_entries WHERE startTime BETWEEN :from AND :to ORDER BY startTime DESC")
    fun observeEntriesInRange(from: Long, to: Long): Flow<List<ActivityEntryEntity>>

    /**
     * FR-1.4: перевірка перекриття лише В МЕЖАХ ОДНІЄЇ категорії — саме тому categoryId
     * у WHERE, а не перевірка проти всіх записів. Різні категорії можуть перекриватись
     * без попередження (напр. Читання + Рух під час прогулянки з аудіокнигою).
     */
    @Query(
        """
        SELECT * FROM activity_entries
        WHERE categoryId = :categoryId
          AND id != :excludeEntryId
          AND (startTime < :endTime AND (startTime + durationMinutes * 60000) > :startTime)
        """
    )
    suspend fun findOverlappingInSameCategory(
        categoryId: String,
        startTime: Long,
        endTime: Long,
        excludeEntryId: String = ""
    ): List<ActivityEntryEntity>

    @Query(
        """
        SELECT SUM(durationMinutes) FROM activity_entries
        WHERE categoryId = :categoryId AND startTime BETWEEN :from AND :to
        """
    )
    suspend fun sumDurationForCategory(categoryId: String, from: Long, to: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ActivityEntryEntity)

    @Update
    suspend fun update(entry: ActivityEntryEntity)

    @Delete
    suspend fun delete(entry: ActivityEntryEntity) // видалення ЗАПИСУ дозволене (FR-1.6), на відміну від категорії
}
