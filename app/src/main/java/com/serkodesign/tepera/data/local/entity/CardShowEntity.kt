package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * T-13 (tepera-dev-spec.md), рівень 3 "історія показів": тип, дата показу, результат. Читається
 * ТІЛЬКИ `CardEngine`/`CardHistoryRepository` для правил частоти — документ прямо забороняє
 * будувати з неї тренди ("Використовується тільки для правил частоти. Будувати з неї тренди
 * заборонено"), тож жоден Stats-екран цю таблицю не читає.
 *
 * [isEstimate] денормалізовано з `CardType.isEstimate` у момент запису — дозволяє рахувати
 * "чи показувалась БУДЬ-ЯКА картка-оцінка цього тижня" одним SQL-запитом без переліку конкретних
 * типів у самому запиті (новий тип картки з `isEstimate = true` автоматично враховується).
 */
@Entity(tableName = "card_show_history")
data class CardShowEntity(
    @PrimaryKey val id: String,
    val cardType: String,
    val isEstimate: Boolean,
    val atMillis: Long,
    val result: String
)
