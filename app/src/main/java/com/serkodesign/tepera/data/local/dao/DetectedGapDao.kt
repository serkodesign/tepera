package com.serkodesign.tepera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity

@Dao
interface DetectedGapDao {

    // OnConflictStrategy.IGNORE + unique index на startTime (DetectedGapEntity): повторне
    // сканування того самого проміжку (FR-D.1) не перезаписує вже позначену/пропущену паузу.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(gaps: List<DetectedGapEntity>)

    // FR-D.3, FR-D.5: лише ще не позначені й не пропущені паузи — картка ніколи не нагадує
    // про те, що користувач уже вирішив ігнорувати.
    @Query(
        "SELECT * FROM detected_gaps WHERE startTime >= :from AND startTime < :to " +
            "AND labeledEntryId IS NULL AND dismissed = 0 ORDER BY startTime"
    )
    suspend fun getUnresolvedInRange(from: Long, to: Long): List<DetectedGapEntity>

    // T-11: непозначені й непропущені кандидати в межах діапазону — ті самі рядки, які
    // getUnresolvedInRange() повернула б. Видаляються ПЕРЕД повторним insertAll() у
    // replaceUnresolvedInRange(), інакше зміна пресету чутливості лише ДОДАВАЛА б нові кандидати
    // поверх застарілих (з іншими startTime за старим пресетом), а не замінювала їх.
    @Query(
        "DELETE FROM detected_gaps WHERE startTime >= :from AND startTime < :to " +
            "AND labeledEntryId IS NULL AND dismissed = 0"
    )
    suspend fun deleteUnresolvedInRange(from: Long, to: Long)

    @Query("UPDATE detected_gaps SET labeledEntryId = :entryId WHERE id = :gapId")
    suspend fun markLabeled(gapId: String, entryId: String)

    @Query("UPDATE detected_gaps SET dismissed = 1 WHERE id = :gapId")
    suspend fun markDismissed(gapId: String)

    /**
     * T-11: замінює непозначені/непропущені кандидати в [from, to) на щойно пораховані [gaps] —
     * позначені й пропущені рядки в цьому діапазоні не чіпає (deleteUnresolvedInRange() відсіює
     * їх власним WHERE). Один `@Transaction`, щоб проміжного стану "видалено, ще не вставлено"
     * не побачив паралельний читач (getUnresolvedInRange() з іншого refresh()).
     */
    @Transaction
    suspend fun replaceUnresolvedInRange(from: Long, to: Long, gaps: List<DetectedGapEntity>) {
        deleteUnresolvedInRange(from, to)
        insertAll(gaps)
    }
}
