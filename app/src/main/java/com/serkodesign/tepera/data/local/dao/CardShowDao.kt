package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.serkodesign.tepera.data.local.entity.CardShowEntity

@Dao
interface CardShowDao {
    @Insert
    suspend fun insert(entry: CardShowEntity)

    /** Останній запис із результатом, ВІДМІННИМ від SHOWN (ANSWERED/SKIPPED) — для мінімального інтервалу. */
    @Query(
        "SELECT * FROM card_show_history WHERE cardType = :cardType AND result != 'SHOWN' " +
            "ORDER BY atMillis DESC LIMIT 1"
    )
    suspend fun getLastResolved(cardType: String): CardShowEntity?

    /** Останній запис БУДЬ-ЯКОГО результату для цього типу — для дебаунсу повторного SHOWN того самого дня. */
    @Query("SELECT * FROM card_show_history WHERE cardType = :cardType ORDER BY atMillis DESC LIMIT 1")
    suspend fun getLatestAny(cardType: String): CardShowEntity?

    // Строго ">" (не "≥") — узгоджено з CardEngine.minIntervalDays (перевірка `< interval`,
    // теж строга): повтор РІВНО через 7 днів після показу мусить бути дозволений обома правилами
    // одночасно, інакше картка з мінімальним інтервалом 7 днів і глобальний тижневий бюджет
    // розходяться в межевому випадку (перевірено юніт-тестом CardEngineTest).
    @Query("SELECT EXISTS(SELECT 1 FROM card_show_history WHERE isEstimate = 1 AND atMillis > :sinceMillis)")
    suspend fun hasEstimateShownSince(sinceMillis: Long): Boolean
}
