package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.NudgeAction
import com.serkodesign.tepera.data.local.entity.NudgeEventEntity

@Dao
interface NudgeEventDao {
    @Insert
    suspend fun insert(event: NudgeEventEntity)

    @Query("UPDATE nudge_events SET shownAt = :shownAt WHERE id = :id")
    suspend fun markShown(id: String, shownAt: Long)

    @Query("UPDATE nudge_events SET action = :action WHERE id = :id")
    suspend fun setAction(id: String, action: NudgeAction)

    @Query("SELECT * FROM nudge_events WHERE scheduledAt >= :fromMillis AND scheduledAt < :toMillis ORDER BY scheduledAt")
    suspend fun getScheduledBetween(fromMillis: Long, toMillis: Long): List<NudgeEventEntity>

    @Query("SELECT COUNT(*) FROM nudge_events WHERE action = :action AND scheduledAt >= :fromMillis AND scheduledAt < :toMillis")
    suspend fun countByAction(action: NudgeAction, fromMillis: Long, toMillis: Long): Int

    @Query("SELECT COUNT(*) FROM nudge_events WHERE scheduledAt >= :fromMillis AND scheduledAt < :toMillis")
    suspend fun countScheduled(fromMillis: Long, toMillis: Long): Int

    @Query("SELECT COUNT(*) FROM nudge_events WHERE shownAt IS NOT NULL AND scheduledAt >= :fromMillis AND scheduledAt < :toMillis")
    suspend fun countShown(fromMillis: Long, toMillis: Long): Int
}
