package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.ActivityEntryDao
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import kotlinx.coroutines.flow.Flow

sealed class SaveEntryResult {
    data object Success : SaveEntryResult()
    data class OverlapDetected(val existing: List<ActivityEntryEntity>) : SaveEntryResult()
}

interface ActivityRepository {
    fun observeEntriesInRange(from: Long, to: Long): Flow<List<ActivityEntryEntity>>
    suspend fun sumDurationForCategory(categoryId: String, from: Long, to: Long): Int
    suspend fun addEntry(entry: ActivityEntryEntity, forceOverwrite: Boolean = false): SaveEntryResult
    suspend fun update(entry: ActivityEntryEntity)
    suspend fun delete(entry: ActivityEntryEntity)
}

class RoomActivityRepository(
    private val dao: ActivityEntryDao
) : ActivityRepository {

    override fun observeEntriesInRange(from: Long, to: Long): Flow<List<ActivityEntryEntity>> =
        dao.observeEntriesInRange(from, to)

    override suspend fun sumDurationForCategory(categoryId: String, from: Long, to: Long): Int =
        dao.sumDurationForCategory(categoryId, from, to) ?: 0

    /**
     * FR-1.4: перевірка перекриття лише в межах ОДНІЄЇ категорії (див. DAO-запит).
     * Записи в різних категоріях зберігаються без будь-якої перевірки.
     */
    override suspend fun addEntry(entry: ActivityEntryEntity, forceOverwrite: Boolean): SaveEntryResult {
        if (!forceOverwrite) {
            val endTime = entry.startTime + entry.durationMinutes * 60_000L
            val overlapping = dao.findOverlappingInSameCategory(
                categoryId = entry.categoryId,
                startTime = entry.startTime,
                endTime = endTime,
                excludeEntryId = entry.id
            )
            if (overlapping.isNotEmpty()) {
                return SaveEntryResult.OverlapDetected(overlapping)
            }
        }
        dao.insert(entry)
        return SaveEntryResult.Success
    }

    override suspend fun update(entry: ActivityEntryEntity) = dao.update(entry)

    override suspend fun delete(entry: ActivityEntryEntity) = dao.delete(entry)
}
