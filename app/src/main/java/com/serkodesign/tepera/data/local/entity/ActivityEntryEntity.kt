package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Один запис офлайн-активності.
 *
 * startTime — epoch ms у ЛОКАЛЬНОМУ часі пристрою, без обробки timezone offset
 * (відоме обмеження MVP, SRS FR-3.1).
 *
 * note — до 250 символів (FR-1.1), валідація на рівні UI/ViewModel, не в БД.
 *
 * onDelete = CASCADE — лише db-рівнева страховка. З UI шлях до справжнього видалення
 * категорії відсутній (FR-2.4), тож на практиці ця гілка не має спрацьовувати в MVP.
 */
@Entity(
    tableName = "activity_entries",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["categoryId"]), Index(value = ["startTime"])]
)
data class ActivityEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val startTime: Long,
    val durationMinutes: Int,
    val note: String? = null,
    val source: EntrySource = EntrySource.MANUAL,
    val createdAt: Long = System.currentTimeMillis()
)
