package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isHidden = 0 ORDER BY sortOrder ASC")
    fun observeActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    // FR-2.4: жодного справжнього delete-запиту — "видалення" це update isHidden = true
    // через update() вище. Метод нижче лишається лише для потенційного майбутнього
    // адмін/debug-функціоналу, з UI MVP не викликається.
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun hardDeleteNotUsedInMvpUi(id: String)
}
