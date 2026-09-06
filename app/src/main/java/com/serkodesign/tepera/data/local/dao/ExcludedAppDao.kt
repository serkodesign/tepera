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

    // FR-6.2: одноразовий зчит для JSON-експорту.
    @Query("SELECT * FROM excluded_apps")
    suspend fun getAllOnce(): List<ExcludedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ExcludedAppEntity)

    // FR-6.2: імпорт JSON-бекапу.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<ExcludedAppEntity>)

    @Delete
    suspend fun delete(app: ExcludedAppEntity)

    @Query("DELETE FROM excluded_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    // FR-6.2: очищення перед імпортом JSON-бекапу (повна заміна, не злиття).
    @Query("DELETE FROM excluded_apps")
    suspend fun deleteAll()
}
