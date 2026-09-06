package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.ExcludedAppDao
import com.serkodesign.tepera.data.local.entity.ExcludedAppEntity
import kotlinx.coroutines.flow.Flow

interface ExcludedAppRepository {
    fun observeAll(): Flow<List<ExcludedAppEntity>>
    suspend fun add(packageName: String, label: String)
    suspend fun remove(app: ExcludedAppEntity)
    suspend fun removeByPackageName(packageName: String)
}

class RoomExcludedAppRepository(
    private val dao: ExcludedAppDao
) : ExcludedAppRepository {

    override fun observeAll(): Flow<List<ExcludedAppEntity>> = dao.observeAll()

    override suspend fun add(packageName: String, label: String) =
        dao.insert(ExcludedAppEntity(packageName = packageName, cachedLabel = label))

    override suspend fun remove(app: ExcludedAppEntity) = dao.delete(app)

    override suspend fun removeByPackageName(packageName: String) = dao.deleteByPackageName(packageName)
}
