package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * v3.0 MVP, Етап 1: план «якщо — то» для години ризику, дослівно словами людини (підказки живуть лише в UI).
 * Один план на вікно; видалення вікна видаляє план.
 */
@Entity(
    tableName = "plans",
    foreignKeys = [
        ForeignKey(
            entity = RiskWindowEntity::class,
            parentColumns = ["id"],
            childColumns = ["windowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["windowId"], unique = true)]
)
data class PlanEntity(
    @PrimaryKey val id: String,
    val windowId: String,
    val ifText: String,
    val thenText: String,
    val createdAt: Long,
    val updatedAt: Long
)
