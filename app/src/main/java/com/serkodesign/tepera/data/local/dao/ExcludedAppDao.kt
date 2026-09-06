package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.ExcludedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedAppDao {

    @Query("SELECT * FROM excluded_apps ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ExcludedAppEntity>>

    @Query("SELECT packageName FROM excluded_apps")
    suspend fun getExcludedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ExcludedAppEntity)

    @Delete
    suspend fun delete(app: ExcludedAppEntity)

    @Query("DELETE FROM excluded_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}
