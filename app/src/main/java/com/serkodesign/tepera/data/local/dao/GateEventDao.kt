package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.GateEventEntity

@Dao
interface GateEventDao {
    @Insert
    suspend fun insert(entry: GateEventEntity)

    @Query(
        "SELECT COUNT(*) FROM gate_events WHERE result = :result " +
            "AND atMillis >= :fromMillis AND atMillis < :toMillis"
    )
    suspend fun countByResultInRange(result: String, fromMillis: Long, toMillis: Long): Int
}
