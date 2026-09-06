package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Застосунок, виключений з підрахунку Online-часу (FR-3.5).
 * Приклади з SRS: карти, е-читалки, банківські застосунки — легітимне використання екрана,
 * яке не варто зараховувати як "залежність".
 *
 * cachedLabel зберігається окремо від системного PackageManager, щоб UI не залежав від
 * повторного (потенційно повільного) запиту назви застосунку при кожному відображенні списку.
 */
@Entity(tableName = "excluded_apps")
data class ExcludedAppEntity(
    @PrimaryKey val packageName: String,
    val cachedLabel: String,
    val addedAt: Long = System.currentTimeMillis()
)
