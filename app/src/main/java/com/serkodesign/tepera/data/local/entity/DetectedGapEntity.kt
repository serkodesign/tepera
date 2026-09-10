package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Виявлена пауза без активності на передньому плані (FR-D.1, SRS v2.6) — кандидат на ручне
 * позначення категорією (FR-D.5). Непозначені й непропущені паузи лишаються в БД безстроково,
 * застосунок ніколи про них не нагадує сам (лише показує в дозволеному вікні опитування,
 * FR-D.3) — повторне сканування того самого проміжку часу (unique index на startTime) не
 * створює дублікат і не чіпає вже виставлені labeledEntryId/dismissed.
 */
@Entity(tableName = "detected_gaps", indices = [Index(value = ["startTime"], unique = true)])
data class DetectedGapEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val durationMinutes: Int, // >= 30 (FR-D.1)
    val labeledEntryId: String? = null, // id створеного ActivityEntry, якщо позначено (FR-D.5)
    val dismissed: Boolean = false // користувач свідомо пропустив; не питати повторно
)
