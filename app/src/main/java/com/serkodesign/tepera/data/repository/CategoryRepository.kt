package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.CategoryDao
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * NFR-7.2: ViewModel звертається лише сюди, ніколи напряму до CategoryDao.
 * Коли (якщо) з'явиться хмарна синхронізація — підміняється лише реалізація нижче,
 * ViewModel і UI цього не помічають.
 */
interface CategoryRepository {
    fun observeActiveCategories(): Flow<List<CategoryEntity>>
    fun observeAllCategories(): Flow<List<CategoryEntity>>
    suspend fun getById(id: String): CategoryEntity?
    suspend fun create(category: CategoryEntity)
    suspend fun archive(categoryId: String) // FR-2.4: єдиний спосіб "видалення" з UI
    suspend fun unarchive(categoryId: String) // повернення з архіву — зворотна дія до archive()
    suspend fun update(category: CategoryEntity)
    suspend fun ensureDefaultsSeeded(defaults: List<CategoryEntity>)

    /**
     * За прямим запитом користувача: справжнє видалення, не архівація — лише для КАСТОМНИХ
     * категорій (`isCustom`). Дефолтні категорії свідомо виключені: `TeperaApp.onCreate()`
     * повторно засіває дефолти щозапуску (`ensureDefaultsSeeded()`,
     * `OnConflictStrategy.IGNORE` за фіксованим id) — реальне видалення дефолтної категорії
     * непомітно "воскресло" б на наступному запуску, архівація для них лишається єдиним
     * коректним способом сховати. Для кастомної категорії видалення звільняє слот ліміту
     * (T-8: до двох), чого архівація не робить (`FK onDelete = CASCADE` прибирає й записи
     * активностей цієї категорії — про це попереджає діалог підтвердження в UI).
     */
    suspend fun deleteCustomCategory(categoryId: String)
}

class RoomCategoryRepository(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun observeActiveCategories(): Flow<List<CategoryEntity>> =
        dao.observeActiveCategories()

    override fun observeAllCategories(): Flow<List<CategoryEntity>> =
        dao.observeAllCategories()

    override suspend fun getById(id: String): CategoryEntity? = dao.getById(id)

    override suspend fun create(category: CategoryEntity) = dao.insert(category)

    override suspend fun archive(categoryId: String) {
        val category = dao.getById(categoryId) ?: return
        dao.update(category.copy(isHidden = true))
    }

    override suspend fun unarchive(categoryId: String) {
        val category = dao.getById(categoryId) ?: return
        dao.update(category.copy(isHidden = false))
    }

    override suspend fun update(category: CategoryEntity) = dao.update(category)

    override suspend fun ensureDefaultsSeeded(defaults: List<CategoryEntity>) =
        dao.insertDefaults(defaults)

    override suspend fun deleteCustomCategory(categoryId: String) {
        val category = dao.getById(categoryId) ?: return
        if (!category.isCustom) return
        dao.hardDeleteNotUsedInMvpUi(categoryId)
    }
}
