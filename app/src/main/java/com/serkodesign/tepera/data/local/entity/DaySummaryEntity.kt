package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P-2 (`CLAUDE-CODE-PLAN-pro.md`): один рядок на ЗАВЕРШЕНУ календарну добу, яку системна
 * історія `UsageStatsManager` покривала ПОВНІСТЮ в момент запису. Рядок після запису НЕ
 * змінюється (той самий принцип, що колишня `daily_snapshots`, D-24/D-25) — `DaySummaryDao.
 * insert()` використовує `OnConflictStrategy.IGNORE`. Жодних назв пакетів застосунків не
 * зберігається (SRS, розділ 7, "топ застосунків — не робимо"). Дані тут ніде не показуються в
 * безкоштовному UI — лише накопичуються для майбутньої Pro-статистики "Місяць"/"Рік" (P-11/P-12).
 *
 * [date] — epoch-day ([java.time.LocalDate.toEpochDay]), первинний ключ.
 * [unlocks] — `null` на API < 28 (`UnlockRepository.isSupported()` false), НЕ 0 — 0 було б
 * хибним фактом ("жодного розблокування" проти "дані недоступні").
 * [dayStartMinute]/[lastUseMinute] — хвилини від півночі ЦІЄЇ календарної доби; `null`, якщо за
 * добу немає жодного суттєвого розблокування/сесії, з яких це можна порахувати.
 * [onlineByHour] — 24 значення (Online-хвилини в кожну годину доби,
 * `BalanceRepository.getOnlineMinutesPerSlot()`) для майбутньої річної теплової карти (P-12).
 * [exclusionHash]/[sleepWindowHash] — хеш Exclusion List / увімкнених вікон сну на момент
 * підрахунку — щоб за потреби розпізнати доби, пораховані за іншими налаштуваннями (без
 * автоматичного перерахунку: рядок незмінний).
 */
@Entity(tableName = "day_summaries")
data class DaySummaryEntity(
    @PrimaryKey val date: Long,
    val onlineMinutes: Int,
    val unlocks: Int?,
    val dayStartMinute: Int?,
    val lastUseMinute: Int?,
    val onlineByHour: IntArray,
    val exclusionHash: Int,
    val sleepWindowHash: Int,
    val createdAt: Long
) {
    // IntArray порівнюється за посиланням у згенерованих data-class equals/hashCode за
    // замовчуванням — явно перевизначено, інакше Room-дедуплікація в тестах і будь-яке порівняння
    // двох однакових за змістом рядків (напр. у юніт-тестах) неправильно вважає їх різними.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DaySummaryEntity) return false
        return date == other.date &&
            onlineMinutes == other.onlineMinutes &&
            unlocks == other.unlocks &&
            dayStartMinute == other.dayStartMinute &&
            lastUseMinute == other.lastUseMinute &&
            onlineByHour.contentEquals(other.onlineByHour) &&
            exclusionHash == other.exclusionHash &&
            sleepWindowHash == other.sleepWindowHash &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + onlineMinutes
        result = 31 * result + (unlocks ?: 0)
        result = 31 * result + (dayStartMinute ?: 0)
        result = 31 * result + (lastUseMinute ?: 0)
        result = 31 * result + onlineByHour.contentHashCode()
        result = 31 * result + exclusionHash
        result = 31 * result + sleepWindowHash
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
