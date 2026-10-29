package com.serkodesign.tepera.domain.risk

import com.serkodesign.tepera.domain.risk.ActivitySuggestions.Id
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ActivitySuggestionsTest {
    private val all = setOf("default-nature", "default-reading", "default-hobby", "default-movement", "default-social", "default-errands")
    private val now = 1_000_000_000_000L

    private fun pick(
        part: PartOfDay,
        weekend: Boolean = false,
        available: Set<String> = all,
        declined: Map<Id, Long> = emptyMap(),
        exclude: Set<Id> = emptySet(),
        seed: Int = 1
    ) = ActivitySuggestions.pick(part, weekend, available, declined, exclude, now, Random(seed))

    @Test
    fun `late hours never suggest movement`() {
        for (part in listOf(PartOfDay.EVENING, PartOfDay.NIGHT)) {
            assertTrue(ActivitySuggestions.pool(part, false).none { it.categoryId == "default-movement" || it.id == Id.WALK })
        }
    }

    @Test
    fun `evening can suggest a book that starts the reading category`() {
        val book = ActivitySuggestions.pool(PartOfDay.EVENING, false).first { it.id == Id.BOOK }
        assertEquals("default-reading", book.categoryId)
    }

    @Test
    fun `suggestion with an archived category is never picked`() {
        val noReading = all - "default-reading"
        repeat(30) { seed ->
            assertFalse(pick(PartOfDay.EVENING, available = noReading, seed = seed)!!.id == Id.BOOK)
        }
    }

    @Test
    fun `recently declined suggestion is skipped`() {
        val declined = mapOf(Id.BOOK to now - 3_600_000L)
        repeat(30) { seed -> assertFalse(pick(PartOfDay.EVENING, declined = declined, seed = seed)!!.id == Id.BOOK) }
    }

    @Test
    fun `old decline no longer counts`() {
        val declined = mapOf(Id.BOOK to now - 4 * 24L * 3_600_000L)
        val seen = (0 until 80).map { pick(PartOfDay.EVENING, declined = declined, seed = it)!!.id }.toSet()
        assertTrue(Id.BOOK in seen)
    }

    @Test
    fun `another idea never repeats the one just shown`() {
        repeat(30) { seed -> assertFalse(pick(PartOfDay.NIGHT, exclude = setOf(Id.BOOK), seed = seed)!!.id == Id.BOOK) }
    }

    @Test
    fun `when everything was declined something is still offered`() {
        val declined = ActivitySuggestions.pool(PartOfDay.NIGHT, false).associate { it.id to now }
        assertNotNull(pick(PartOfDay.NIGHT, declined = declined))
    }

    @Test
    fun `without matching categories only suggestions without a category remain`() {
        val picked = (0 until 40).map { pick(PartOfDay.EVENING, available = emptySet(), seed = it)!! }
        assertTrue(picked.all { it.categoryId == null })
    }

    @Test
    fun `when exclusion eats the whole pool the usable ones are offered again`() {
        val picked = ActivitySuggestions.pick(PartOfDay.DAY, false, emptySet(), emptyMap(), setOf(Id.WATER, Id.BREATHE), now)
        assertNotNull(picked)
        assertNull(picked!!.categoryId)
    }
}
