package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity

/**
 * v3.0 MVP, Етап 0/1 (`docs/CLAUDE-CODE-PLAN-v3-MVP.md`): секунди в одному застосунку-стрічці за 15-хвилинний слот
 * локальної календарної дати. Лише ненульові слоти; лише застосунки зі списку стрічок, підтвердженого людиною.
 * Назва пакета зберігається, щоб зміна списку стрічок не вимагала нового збору (історія UsageStats коротка) і щоб
 * вікно ризику могло показати «топ застосунків» (Етап 3, M6). Дані не залишають пристрій.
 *
 * [date] — epoch-day ([java.time.LocalDate.toEpochDay]); [slot] — 0..95 (0 = 00:00–00:15 за настінним годинником).
 */
@Entity(tableName = "usage_slots", primaryKeys = ["date", "slot", "packageName"])
data class UsageSlotEntity(
    val date: Long,
    val slot: Int,
    val packageName: String,
    val seconds: Int
)
