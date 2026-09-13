package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.EstimateType
import com.serkodesign.tepera.data.local.entity.UserEstimateEntity

@Dao
interface UserEstimateDao {

    @Insert
    suspend fun insert(estimate: UserEstimateEntity)

    // Найновіший рядок цього типу, для якого ще не порахований actualValue (FR-D.2-подібний
    // принцип пасивного сорому: реальне число з'являється лише коли є з чим його поставити поруч).
    @Query(
        "SELECT * FROM user_estimates WHERE type = :type AND actualValue IS NULL " +
            "ORDER BY createdAt DESC LIMIT 1"
    )
    suspend fun getLatestUnresolved(type: EstimateType): UserEstimateEntity?

    @Query("UPDATE user_estimates SET actualValue = :actualValue WHERE id = :id")
    suspend fun setActualValue(id: String, actualValue: Long)
}
