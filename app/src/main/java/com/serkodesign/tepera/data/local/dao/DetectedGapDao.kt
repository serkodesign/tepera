package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity

@Dao
interface DetectedGapDao {

    // OnConflictStrategy.IGNORE + unique index на startTime (DetectedGapEntity): повторне
    // сканування того самого проміжку (FR-D.1) не перезаписує вже позначену/пропущену паузу.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(gaps: List<DetectedGapEntity>)

    // FR-D.3, FR-D.5: лише ще не позначені й не пропущені паузи — картка ніколи не нагадує
    // про те, що користувач уже вирішив ігнорувати.
    @Query(
        "SELECT * FROM detected_gaps WHERE startTime >= :from AND startTime < :to " +
            "AND labeledEntryId IS NULL AND dismissed = 0 ORDER BY startTime"
    )
    suspend fun getUnresolvedInRange(from: Long, to: Long): List<DetectedGapEntity>

    @Query("UPDATE detected_gaps SET labeledEntryId = :entryId WHERE id = :gapId")
    suspend fun markLabeled(gapId: String, entryId: String)

    @Query("UPDATE detected_gaps SET dismissed = 1 WHERE id = :gapId")
    suspend fun markDismissed(gapId: String)
}
