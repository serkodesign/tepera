package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

/**
 * T-3 (tepera-dev-spec.md), розділ 2.2 "принцип пасивного сорому": число, здатне засмутити
 * (Online-час, час останнього дотику, кількість розблокувань), ніколи не подається пасивно —
 * лише через власну оцінку користувача: питання → [estimatedValue] → пізніше [actualValue] →
 * дві цифри поруч, без висновків. [actualValue] лишається `null`, доки не з'явиться доступ до
 * статистики використання — тоді відповідний ViewModel (напр. `OnlineEstimateRevealViewModel`)
 * дораховує його для того самого [forDate] і зберігає, не створюючи новий рядок.
 */
@Entity(tableName = "user_estimates")
data class UserEstimateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: EstimateType,
    val estimatedValue: Long,
    val actualValue: Long?,
    val forDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
)
