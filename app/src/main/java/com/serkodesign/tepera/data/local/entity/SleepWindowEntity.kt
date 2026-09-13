package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * T-12 (tepera-dev-spec.md): вікно сну як ЧИСТО технічний параметр розрахунку — годин, які не
 * входять у добу. НЕ сегмент структури доби, не має власного екрана статистики, не показується
 * на шкалі. [slot] 1 або 2 (друге вікно — для плаваючого графіка, вимкнене за замовчуванням).
 * [startMinuteOfDay]/[endMinuteOfDay] — хвилини від півночі (0-1439); [endMinuteOfDay] <=
 * [startMinuteOfDay] означає, що вікно перетинає північ (напр. 23:00-07:00 → 1380, 420).
 * Вікно, що ЦІЛКОМ лежить у денному часі (нічна зміна, напр. 09:00-16:00), теж підтримується —
 * тоді [endMinuteOfDay] > [startMinuteOfDay].
 */
@Entity(tableName = "sleep_windows")
data class SleepWindowEntity(
    @PrimaryKey val slot: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val enabled: Boolean
)
