package com.serkodesign.tepera.data

import com.serkodesign.tepera.data.local.entity.CategoryEntity

/**
 * FR-2.1: 5 дефолтних категорій, підтверджено в SRS розділ 11 (Природа, Читання, Хобі/Творчість,
 * Рух/Спорт, Сон). id — фіксовані (не UUID.randomUUID()), інакше insertDefaults()
 * з OnConflictStrategy.IGNORE не впізнавав би вже засіяні рядки і плодив дублікати щозапуску.
 */
object DefaultCategories {

    val all: List<CategoryEntity> = listOf(
        CategoryEntity(
            id = "default-nature",
            name = "Природа",
            nameKey = "nature",
            iconName = "nature",
            colorHex = "#4E7A51",
            isDefault = true,
            sortOrder = 0
        ),
        CategoryEntity(
            id = "default-reading",
            name = "Читання",
            nameKey = "reading",
            iconName = "reading",
            colorHex = "#4A6FA5",
            isDefault = true,
            sortOrder = 1
        ),
        CategoryEntity(
            id = "default-hobby",
            name = "Хобі/творчість",
            nameKey = "hobby",
            iconName = "hobby",
            colorHex = "#B08968",
            isDefault = true,
            sortOrder = 2
        ),
        CategoryEntity(
            id = "default-movement",
            name = "Рух/спорт",
            nameKey = "movement",
            iconName = "movement",
            colorHex = "#C9704F",
            isDefault = true,
            sortOrder = 3
        ),
        CategoryEntity(
            id = "default-sleep",
            name = "Сон",
            nameKey = "sleep",
            iconName = "sleep",
            colorHex = "#5C6B73",
            isDefault = true,
            sortOrder = 4
        )
    )
}
