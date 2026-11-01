package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.FeedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: FeedAppEntity)

    @Query("DELETE FROM feed_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM feed_apps ORDER BY label COLLATE NOCASE")
    fun observeAll(): Flow<List<FeedAppEntity>>

    @Query("SELECT packageName FROM feed_apps")
    suspend fun getPackages(): List<String>

    @Query("SELECT COUNT(*) FROM feed_apps")
    suspend fun count(): Int

    /** Резервна копія: усі обрані стрічки. */
    @Query("SELECT * FROM feed_apps")
    suspend fun getAllOnce(): List<FeedAppEntity>

    @Query("DELETE FROM feed_apps")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<FeedAppEntity>)
}
