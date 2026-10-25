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

    @Query("DELETE FROM risk_windows WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM risk_windows WHERE id = :id")
    suspend fun getById(id: String): RiskWindowEntity?

    @Query("UPDATE risk_windows SET isUserDisabled = :disabled, updatedAt = :now WHERE id = :id")
    suspend fun setUserDisabled(id: String, disabled: Boolean, now: Long)

    @Query("UPDATE risk_windows SET suggestedStartMinute = :start, suggestedEndMinute = :end WHERE id = :id")
    suspend fun setShiftSuggestion(id: String, start: Int?, end: Int?)

    @Query(
        "UPDATE risk_windows SET startMinute = suggestedStartMinute, endMinute = suggestedEndMinute, " +
            "suggestedStartMinute = NULL, suggestedEndMinute = NULL, updatedAt = :now " +
            "WHERE id = :id AND suggestedStartMinute IS NOT NULL AND suggestedEndMinute IS NOT NULL"
    )
    suspend fun applyShiftSuggestion(id: String, now: Long)
}
