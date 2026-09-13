package com.serkodesign.tepera.data

import com.serkodesign.tepera.data.local.entity.CategoryEntity

/**
 * FR-2.1 (SRS v2.5): 6 дефолтних категорій — Природа, Читання, Хобі/Творчість, Рух/Спорт,
 * Живе спілкування, Справи (T-8, tepera-dev-spec.md). id — фіксовані (не UUID.randomUUID()),
 * інакше insertDefaults() з OnConflictStrategy.IGNORE не впізнавав би вже засіяні рядки і
 * плодив дублікати щозапуску.
 *
 * "default-sleep" прибрано зі списку у v2.4: без Sleep API вікно сну було здогадкою, поданою
 * як факт (розділ 3.3 SRS). На вже засіяних БД цей рядок не видаляється (FR-2.3 — лише
 * архівація), а архівується один раз при старті — TeperaApp.onCreate(). "default-social"
 * (Живе спілкування) — нова категорія на звільненому слоті.
 *
 * **T-8: "default-errands" (Справи) — нейтральна 6-та категорія.** Обґрунтування документа:
 * попередні п'ять самі по собі є нормою "чим варто заповнювати вільний час" (FR-P.5 забороняє
 * норми) — людина, чий день переважно робота/дорога/побут, бачила своє життя цілком провалене
 * в "Решту дня". `CategoryOnboardingScreen` (T-8) показує всі 6 на онбордингу з перемикачами —
 * саме тому нова категорія сіється УВІМКНЕНОЮ (isHidden = false), як і решта, а не прихованою:
 * онбординг вирішує, що лишити, initial-seed цього не робить сам.
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
        ),
        CategoryEntity(
            id = "default-errands",
            name = "Справи",
            nameKey = "errands",
            iconName = "errands",
            colorHex = "#5C6B73",
            isDefault = true,
            sortOrder = 5
        )
    )

    /** Дефолтна категорія, прибрана з v2.4 — archive(), не видалення (FR-2.3). */
    const val LEGACY_SLEEP_ID = "default-sleep"

    // Іменовані константи для WeeklyDigestViewModel (картка "Цей тиждень") — щоб не дублювати
    // рядкові літерали id категорій у логіці підрахунку.
    const val READING_ID = "default-reading"
    const val MOVEMENT_ID = "default-movement"
}
