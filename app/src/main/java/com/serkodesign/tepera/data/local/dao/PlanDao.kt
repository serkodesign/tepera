package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    /** Один план на вікно (unique-індекс `windowId`): повторне збереження замінює попередній. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: PlanEntity)

    @Query("SELECT * FROM plans WHERE windowId = :windowId")
    suspend fun getForWindow(windowId: String): PlanEntity?

    @Query("SELECT * FROM plans")
    fun observeAll(): Flow<List<PlanEntity>>

    @Query("DELETE FROM plans WHERE windowId = :windowId")
    suspend fun deleteForWindow(windowId: String)
}
