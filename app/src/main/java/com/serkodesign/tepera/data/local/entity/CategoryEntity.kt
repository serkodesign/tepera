package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Категорія офлайн-активності.
 *
 * v2.0: дефолтний набір — 5 категорій (Природа, Читання, Хобі/Творчість, Рух/Спорт, Сон —
 * робочий список, підтвердити перед фазою 1, SRS розділ 11) + isCustom = true категорії, які
 * може додати користувач (FR-2.2). **T-8 (tepera-dev-spec.md): ліміт кастомних категорій
 * піднято з однієї до ДВОХ** (документ прямо задає нове число), плюс додано 6-ту дефолтну
 * "Справи" (`DefaultCategories.kt`). Обмеження кількості кастомних категорій перевіряється на
 * рівні Repository/ViewModel, не в схемі БД.
 *
 * isAutoTracked лишений для сумісності з post-MVP Health Connect/Sleep API — у MVP
 * завжди false, автотрекінгу немає (SRS 2.5).
 *
 * "Видалення" з UI для ДЕФОЛТНОЇ категорії = isHidden = true (FR-2.3, реального видалення
 * рядка не викликається — дефолти пересіваються щозапуску за фіксованим id). **За прямим
 * запитом користувача КАСТОМНА категорія тепер видаляється по-справжньому**
 * (`CategoryRepository.deleteCustomCategory()`, `CategoryDao.hardDeleteNotUsedInMvpUi()`) —
 * звільняє слот ліміту (T-8), чого архівація не робить.
 *
 * nameKey — стабільний ключ ("nature", "reading", ...) для 5 дефолтних категорій, за яким UI
 * резолвить локалізовану назву через strings.xml (UA/EN, CLAUDE.md). null для кастомної
 * категорії — там name зберігає буквальний текст, який ввів користувач. Без nameKey назва
 * дефолтної категорії застигла б у мові пристрою на момент першого запуску застосунку.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameKey: String? = null,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val isCustom: Boolean = false,
    val isAutoTracked: Boolean = false, // post-MVP, завжди false в MVP
    val isHidden: Boolean = false,
    val sortOrder: Int = 0
)
