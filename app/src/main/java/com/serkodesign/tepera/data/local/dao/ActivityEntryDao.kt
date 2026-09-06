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

    // FR-6.2: одноразовий зчит УСІХ записів (не лише в діапазоні) для JSON-експорту.
    @Query("SELECT * FROM activity_entries")
    suspend fun getAllOnce(): List<ActivityEntryEntity>

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

    /** FR-4.6: "5-й слот" — коли востаннє логували цю категорію, щоб підсвітити занедбану (>3 днів). */
    @Query("SELECT MAX(startTime) FROM activity_entries WHERE categoryId = :categoryId")
    suspend fun lastLoggedTime(categoryId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ActivityEntryEntity)

    // FR-6.2: імпорт JSON-бекапу — REPLACE, id записів з бекапу зберігаються буквально.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ActivityEntryEntity>)

    @Update
    suspend fun update(entry: ActivityEntryEntity)

    @Delete
    suspend fun delete(entry: ActivityEntryEntity) // видалення ЗАПИСУ дозволене (FR-1.6), на відміну від категорії

    // FR-6.2: очищення перед імпортом JSON-бекапу (повна заміна, не злиття).
    @Query("DELETE FROM activity_entries")
    suspend fun deleteAll()
}
