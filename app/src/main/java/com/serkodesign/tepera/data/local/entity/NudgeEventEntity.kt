package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * v3.0 MVP, Етап 1: одне заплановане нагадування перед вікном. [shownAt] — `null`, доки сповіщення не показане
 * (або не показане взагалі: людина вже була в стрічці, натиснула «Не сьогодні» тощо). [action] — реакція людини;
 * `NONE` = не реагувала. Частота (не більше одного на добу) рахується з цієї таблиці.
 */
@Entity(
    tableName = "nudge_events",
    foreignKeys = [
        ForeignKey(
            entity = RiskWindowEntity::class,
            parentColumns = ["id"],
            childColumns = ["windowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["windowId"]), Index(value = ["scheduledAt"])]
)
data class NudgeEventEntity(
    @PrimaryKey val id: String,
    val windowId: String,
    val scheduledAt: Long,
    val shownAt: Long?,
    val action: NudgeAction
)

enum class NudgeAction { OK, NOT_TODAY, NONE }
