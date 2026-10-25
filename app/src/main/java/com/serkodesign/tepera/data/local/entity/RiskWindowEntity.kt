package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.serkodesign.tepera.domain.risk.DayType

/**
 * v3.0 MVP, Етап 1: година ризику, знайдена алгоритмом (`domain.risk.RiskWindowFinder`).
 *
 * [startMinute]/[endMinute] — хвилини від локальної півночі; [endMinute] ≤ [startMinute] означає перехід через
 * північ. [recurrenceDays] з [observedDays] і [avgMinutes] — статистика останнього перерахунку (показується в
 * «Чому це вікно?», Етап 3). [topApps] — до трьох пакетів через кому, за часом у вікні. [isUserDisabled] — людина
 * сама вимкнула вікно: перерахунок його статистику оновлює, але прапорець не скидає і вікно не повертає.
 */
@Entity(tableName = "risk_windows")
data class RiskWindowEntity(
    @PrimaryKey val id: String,
    val startMinute: Int,
    val endMinute: Int,
    val dayType: DayType,
    val recurrenceDays: Int,
    val observedDays: Int,
    val avgMinutes: Int,
    val topApps: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isUserDisabled: Boolean
)
