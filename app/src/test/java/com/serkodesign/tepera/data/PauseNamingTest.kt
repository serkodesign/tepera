package com.serkodesign.tepera.data

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.repository.ActivityRepository
import com.serkodesign.tepera.data.repository.SaveEntryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** W-1-style рукописний фейк (як `FakeCardHistorySource` для `CardEngine`) — без Context/Room. */
private class FakeGapStore : PauseGapStore {
    val gaps = mutableMapOf<Long, DetectedGapEntity>() // keyed by startTime

    override suspend fun findOrCreateGap(startTime: Long, durationMinutes: Int): DetectedGapEntity =
        gaps.getOrPut(startTime) { DetectedGapEntity(startTime = startTime, durationMinutes = durationMinutes) }

    override suspend fun findGapByStartTime(startTime: Long): DetectedGapEntity? = gaps[startTime]

    override suspend fun markLabeledAt(gapId: String, entryId: String, atMillis: Long) {
        val entry = gaps.values.first { it.id == gapId }
        gaps[entry.startTime] = entry.copy(labeledEntryId = entryId, labeledAtMillis = atMillis)
    }

    override suspend fun clearLabel(gapId: String) {
        val entry = gaps.values.first { it.id == gapId }
        gaps[entry.startTime] = entry.copy(labeledEntryId = null, labeledAtMillis = null)
    }
}

private class FakeActivityRepository : ActivityRepository {
    val entries = mutableMapOf<String, ActivityEntryEntity>()

    override fun observeEntriesInRange(from: Long, to: Long): Flow<List<ActivityEntryEntity>> = flowOf(entries.values.toList())
    override suspend fun sumDurationForCategory(categoryId: String, from: Long, to: Long): Int = 0
    override suspend fun addEntry(entry: ActivityEntryEntity, forceOverwrite: Boolean): SaveEntryResult {
        entries[entry.id] = entry
        return SaveEntryResult.Success
    }
    override suspend fun update(entry: ActivityEntryEntity) { entries[entry.id] = entry }
    override suspend fun delete(entry: ActivityEntryEntity) { entries.remove(entry.id) }
    override suspend fun getById(id: String): ActivityEntryEntity? = entries[id]
    override suspend fun lastLoggedTime(categoryId: String): Long? = null
    override suspend fun saveInterval(
        categoryId: String, startMillis: Long, endMillis: Long, note: String?,
        replaceIds: List<String>, seriesId: String?, forceOverwrite: Boolean
    ): SaveEntryResult = SaveEntryResult.Success
    override suspend fun getWholeActivity(id: String): List<ActivityEntryEntity> = listOfNotNull(entries[id])
    override suspend fun deleteWholeActivity(entry: ActivityEntryEntity) { entries.remove(entry.id) }
}

/** W-2: `namePause`/`undoPauseNaming` — без Room/Context, проти рукописних фейків. */
class PauseNamingTest {

    @Test
    fun naming_a_fresh_pause_creates_one_labeled_entry() = runBlocking {
        val gapStore = FakeGapStore()
        val activities = FakeActivityRepository()

        val outcome = namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "reading")

        assertTrue(outcome is PauseNamingOutcome.Named)
        assertEquals(1, activities.entries.size)
        val entry = activities.entries.values.single()
        assertEquals("reading", entry.categoryId)
        assertEquals(EntrySource.GAP_LABELED, entry.source)
        assertEquals(entry.id, gapStore.findGapByStartTime(1_000L)?.labeledEntryId)
    }

    @Test
    fun renaming_the_same_pause_updates_the_existing_entry_without_duplicating() = runBlocking {
        val gapStore = FakeGapStore()
        val activities = FakeActivityRepository()

        namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "reading")
        val secondOutcome = namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "nature")

        assertTrue(secondOutcome is PauseNamingOutcome.Relabeled)
        assertEquals(1, activities.entries.size) // не два записи для однієї паузи
        assertEquals("nature", activities.entries.values.single().categoryId)
    }

    @Test
    fun naming_survives_the_gap_row_being_recreated_after_a_rescan() = runBlocking {
        // Імітує T-11 replaceUnresolvedInRange: непозначений рядок видалили й вставили заново з
        // ІНШИМ Room id, але тим самим startTime — все одно має правильно приєднатись.
        val gapStore = FakeGapStore()
        val activities = FakeActivityRepository()
        gapStore.gaps[1_000L] = DetectedGapEntity(startTime = 1_000L, durationMinutes = 45) // "старий" рядок, інший id

        val outcome = namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "reading")

        assertTrue(outcome is PauseNamingOutcome.Named)
        assertEquals(1, activities.entries.size)
    }

    @Test
    fun undo_within_the_window_deletes_the_entry_and_returns_to_neutral() = runBlocking {
        val gapStore = FakeGapStore()
        val activities = FakeActivityRepository()
        namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "reading", nowMillis = 0L)

        val undone = undoPauseNaming(gapStore, activities, startTime = 1_000L, nowMillis = 4 * 60_000L)

        assertTrue(undone)
        assertTrue(activities.entries.isEmpty())
        val gap = gapStore.findGapByStartTime(1_000L)!!
        assertNull(gap.labeledEntryId)
        assertNull(gap.labeledAtMillis)
    }

    @Test
    fun undo_after_five_minutes_does_nothing() = runBlocking {
        val gapStore = FakeGapStore()
        val activities = FakeActivityRepository()
        namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "reading", nowMillis = 0L)

        val undone = undoPauseNaming(gapStore, activities, startTime = 1_000L, nowMillis = 5 * 60_000L + 1)

        assertFalse(undone)
        assertEquals(1, activities.entries.size) // запис лишається
    }

    @Test
    fun undo_after_a_relabel_resets_to_neutral_not_the_previous_category() = runBlocking {
        val gapStore = FakeGapStore()
        val activities = FakeActivityRepository()
        namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "reading", nowMillis = 0L)
        namePause(gapStore, activities, startTime = 1_000L, durationMinutes = 45, categoryId = "nature", nowMillis = 2 * 60_000L)

        // Вікно рахується від ОСТАННЬОГО називання (2 хв), тож 6 хв від старту, але лише 4 хв від relabel — ще діє.
        val undone = undoPauseNaming(gapStore, activities, startTime = 1_000L, nowMillis = 6 * 60_000L)

        assertTrue(undone)
        assertTrue(activities.entries.isEmpty()) // нейтрально, не "reading" назад
    }

    @Test
    fun undo_on_a_pause_that_was_never_named_returns_false() = runBlocking {
        val gapStore = FakeGapStore()
        val activities = FakeActivityRepository()

        assertFalse(undoPauseNaming(gapStore, activities, startTime = 1_000L))
    }
}
