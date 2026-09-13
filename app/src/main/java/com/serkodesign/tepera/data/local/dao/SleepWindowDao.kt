package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepWindowDao {

    @Query("SELECT * FROM sleep_windows ORDER BY slot")
    fun observeAll(): Flow<List<SleepWindowEntity>>

    @Query("SELECT * FROM sleep_windows ORDER BY slot")
    suspend fun getAll(): List<SleepWindowEntity>

    @Upsert
    suspend fun upsert(window: SleepWindowEntity)
}
