package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.AppGateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppGateDao {
    @Query("SELECT * FROM app_gates ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<AppGateEntity>>

    @Query("SELECT * FROM app_gates ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<AppGateEntity>

    @Query("SELECT * FROM app_gates WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AppGateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gate: AppGateEntity)

    @Query("DELETE FROM app_gates WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("UPDATE app_gates SET originalIconHandled = 1 WHERE packageName = :packageName")
    suspend fun markOriginalIconHandled(packageName: String)

    @Query("UPDATE app_gates SET delaySeconds = :delaySeconds WHERE packageName = :packageName")
    suspend fun updateDelaySeconds(packageName: String, delaySeconds: Int)

    @Query("UPDATE app_gates SET lastProceedAtMillis = :millis WHERE packageName = :packageName")
    suspend fun updateLastProceedAtMillis(packageName: String, millis: Long)
}
