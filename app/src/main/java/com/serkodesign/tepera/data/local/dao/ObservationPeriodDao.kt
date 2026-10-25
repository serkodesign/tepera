package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.ObservationPeriodEntity

@Dao
interface ObservationPeriodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(period: ObservationPeriodEntity)

    /** Найновіший період (за початком). */
    @Query("SELECT * FROM observation_periods ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatest(): ObservationPeriodEntity?
}
