package com.serkodesign.tepera.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/** T-3 (tepera-dev-spec.md): Room не підтримує `java.time.LocalDate` нативно — зберігається як epoch-day. */
class Converters {
    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    /** P-2 (`CLAUDE-CODE-PLAN-pro.md`): `DaySummaryEntity.onlineByHour` (24 значення) — Room не
     * підтримує `IntArray` нативно, зберігається як рядок, розділений комами. */
    @TypeConverter
    fun fromIntArray(hours: IntArray?): String? = hours?.joinToString(",")

    @TypeConverter
    fun toIntArray(value: String?): IntArray? = value?.split(",")?.map { it.toInt() }?.toIntArray()
}
