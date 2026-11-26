package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import java.time.LocalDate

/**
 * Аналітика, рівень 1: денний лічильник події. Жодних окремих подій із точним часом — лише «скільки разів за добу».
 * [propsKey] — канонічний рядок властивостей (`"tab=data"`, відсортовані ключі, лише з allowlist), порожній, якщо їх нема.
 * Зберігається тільки на пристрої; видаляється через 90 днів і разом із «Видалити всі дані».
 */
@Entity(
    tableName = "daily_event_counters",
    primaryKeys = ["date", "eventName", "propsKey"]
)
data class DailyEventCounterEntity(
    val date: LocalDate,
    val eventName: String,
    val propsKey: String,
    val count: Int
)
