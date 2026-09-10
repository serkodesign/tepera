package com.serkodesign.tepera.data

import com.serkodesign.tepera.data.local.entity.CategoryEntity

/**
 * FR-2.1 (SRS v2.5): 5 дефолтних категорій — Природа, Читання, Хобі/Творчість, Рух/Спорт,
 * Живе спілкування. id — фіксовані (не UUID.randomUUID()), інакше insertDefaults()
 * з OnConflictStrategy.IGNORE не впізнавав би вже засіяні рядки і плодив дублікати щозапуску.
 *
 * "default-sleep" прибрано зі списку у v2.4: без Sleep API вікно сну було здогадкою, поданою
 * як факт (розділ 3.3 SRS). На вже засіяних БД цей рядок не видаляється (FR-2.3 — лише
 * архівація), а архівується один раз при старті — TeperaApp.onCreate(). "default-social"
 * (Живе спілкування) — нова категорія на звільненому слоті.
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
            id = "default-social",
            name = "Живе спілкування",
            nameKey = "social",
            iconName = "social",
            colorHex = "#8A5A83",
            isDefault = true,
            sortOrder = 4
        )
    )

    /** Дефолтна категорія, прибрана з v2.4 — archive(), не видалення (FR-2.3). */
    const val LEGACY_SLEEP_ID = "default-sleep"

    // Іменовані константи для WeeklyDigestViewModel (картка "Цей тиждень") — щоб не дублювати
    // рядкові літерали id категорій у логіці підрахунку.
    const val READING_ID = "default-reading"
    const val MOVEMENT_ID = "default-movement"
}
