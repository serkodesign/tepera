package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.ActivityEntryDao
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.util.splitAtDayRollover
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
    suspend fun getById(id: String): ActivityEntryEntity?
    suspend fun lastLoggedTime(categoryId: String): Long?
    /**
     * Зберігає активність як інтервал [startMillis, endMillis): при потребі ділить на частини по
     * логічних добах (`splitAtDayRollover`) зі спільним `seriesId`. [replaceIds] — id записів, які ця
     * активність замінює (редагування): вони не рахуються перекриттям і після збереження зникають.
     */
    suspend fun saveInterval(
        categoryId: String,
        startMillis: Long,
        endMillis: Long,
        note: String?,
        replaceIds: List<String> = emptyList(),
        seriesId: String? = null,
        forceOverwrite: Boolean = false
    ): SaveEntryResult

    /** Запис і всі його «сусідні» частини, якщо це багатодобова активність (інакше — лише він сам). */
    suspend fun getWholeActivity(id: String): List<ActivityEntryEntity>

    /** Видаляє запис разом з усіма частинами його багатодобової активності. */
    suspend fun deleteWholeActivity(entry: ActivityEntryEntity)
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

    override suspend fun getById(id: String): ActivityEntryEntity? = dao.getById(id)

    override suspend fun lastLoggedTime(categoryId: String): Long? = dao.lastLoggedTime(categoryId)

    override suspend fun saveInterval(
        categoryId: String,
        startMillis: Long,
        endMillis: Long,
        note: String?,
        replaceIds: List<String>,
        seriesId: String?,
        forceOverwrite: Boolean
    ): SaveEntryResult {
        val parts = splitAtDayRollover(startMillis, endMillis)
        require(parts.isNotEmpty()) { "interval end must be after start" }

        if (!forceOverwrite) {
            val overlapping = parts
                .flatMap { dao.findOverlappingExcluding(categoryId, it.startMillis, it.endMillis, replaceIds) }
                .distinctBy { it.id }
            if (overlapping.isNotEmpty()) return SaveEntryResult.OverlapDetected(overlapping)
        }

        val sharedSeriesId = if (parts.size > 1) seriesId ?: UUID.randomUUID().toString() else null
        // Id замінюваних записів перевикористовуються по черзі (REPLACE збереже рядок), зайві видаляються.
        val entries = parts.mapIndexed { index, part ->
            ActivityEntryEntity(
                id = replaceIds.getOrNull(index) ?: UUID.randomUUID().toString(),
                categoryId = categoryId,
                startTime = part.startMillis,
                durationMinutes = part.minutes,
                note = note,
                seriesId = sharedSeriesId
            )
        }
        dao.replaceAll(deleteIds = replaceIds.drop(parts.size), entries = entries)
        return SaveEntryResult.Success
    }

    override suspend fun getWholeActivity(id: String): List<ActivityEntryEntity> {
        val entry = dao.getById(id) ?: return emptyList()
        val series = entry.seriesId ?: return listOf(entry)
        return dao.getBySeriesId(series).ifEmpty { listOf(entry) }
    }

    override suspend fun deleteWholeActivity(entry: ActivityEntryEntity) {
        val series = entry.seriesId
        if (series != null) dao.deleteBySeriesId(series) else dao.delete(entry)
    }
}
