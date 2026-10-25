package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.serkodesign.tepera.data.local.entity.RiskWindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RiskWindowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: RiskWindowEntity)

    @Update
    suspend fun update(window: RiskWindowEntity)

    @Query("SELECT * FROM risk_windows ORDER BY avgMinutes DESC")
    suspend fun getAll(): List<RiskWindowEntity>

    @Query("SELECT * FROM risk_windows ORDER BY avgMinutes DESC")
    fun observeAll(): Flow<List<RiskWindowEntity>>

    @Query("SELECT * FROM risk_windows WHERE id = :id")
    suspend fun getById(id: String): RiskWindowEntity?

    @Query("UPDATE risk_windows SET isUserDisabled = :disabled, updatedAt = :now WHERE id = :id")
    suspend fun setUserDisabled(id: String, disabled: Boolean, now: Long)
}
