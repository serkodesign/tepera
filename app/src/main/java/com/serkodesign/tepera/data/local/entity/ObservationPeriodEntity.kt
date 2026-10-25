package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * v3.0 MVP, Етап 1: тиждень спостереження без втручань — базова лінія для порівняння «хвилини у вікнах цього
 * тижня проти тижня спостереження» (Етап 5). Дати — epoch-day локальної дати ([java.time.LocalDate.toEpochDay]);
 * [endDate] = `null`, доки період триває.
 */
@Entity(tableName = "observation_periods")
data class ObservationPeriodEntity(
    @PrimaryKey val id: String,
    val startDate: Long,
    val endDate: Long?
)
