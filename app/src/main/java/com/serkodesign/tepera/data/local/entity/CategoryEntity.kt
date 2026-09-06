package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Категорія офлайн-активності.
 *
 * v2.0: дефолтний набір — 5 категорій (Природа, Читання, Хобі/Творчість, Рух/Спорт, Сон —
 * робочий список, підтвердити перед фазою 1, SRS розділ 11) + рівно ОДНА isCustom = true
 * категорія, яку може додати користувач (FR-2.2). Обмеження кількості кастомних категорій
 * перевіряється на рівні Repository, не в схемі БД.
 *
 * isAutoTracked лишений для сумісності з post-MVP Health Connect/Sleep API — у MVP
 * завжди false, автотрекінгу немає (SRS 2.5).
 *
 * "Видалення" з UI = isHidden = true, однаково для дефолтних і кастомної категорії (FR-2.3).
 * Реального видалення рядка з UI не викликається.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val isCustom: Boolean = false,
    val isAutoTracked: Boolean = false, // post-MVP, завжди false в MVP
    val isHidden: Boolean = false,
    val sortOrder: Int = 0
)
