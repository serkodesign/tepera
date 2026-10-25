package com.serkodesign.tepera.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.local.entity.UsageSlotEntity
import com.serkodesign.tepera.data.repository.RiskWindowRepository
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.RiskCandidate
import com.serkodesign.tepera.domain.risk.RiskResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RiskWindowRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RiskWindowRepository

    private val last = LocalDate.of(2026, 10, 20)
    private val days = (6 downTo 0).map { last.minusDays(it.toLong()) }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = RiskWindowRepository(db.riskWindowDao(), db.planDao(), db.usageSlotDao())
    }

    @After
    fun tearDown() = db.close()

    private fun candidate(start: Int = 22 * 60, end: Int = 23 * 60 + 30, avg: Int = 36, recurrence: Int = 6) =
        RiskCandidate(start, end, recurrence, 7, avg, days.drop(1), DayType.ALL)

    private fun result(vararg c: RiskCandidate, enough: Boolean = true) = RiskResult(days, 7, enough, c.toList())

    @Test
    fun saveDetectedInsertsWindowWithTopAppsFromItsSlots() = runBlocking {
        db.usageSlotDao().insertAll(
            days.flatMap { d ->
                listOf(
                    UsageSlotEntity(d.toEpochDay(), 88, "a.feed", 600),
                    UsageSlotEntity(d.toEpochDay(), 89, "b.feed", 100),
                    UsageSlotEntity(d.toEpochDay(), 50, "z.feed", 900) // поза вікном
                )
            }
        )
        val saved = repo.saveDetected(result(candidate()))
        assertEquals(1, saved.size)
        assertEquals("a.feed,b.feed", saved.single().topApps)
        assertEquals(6, saved.single().recurrenceDays)
    }

    @Test
    fun saveDetectedAgainUpdatesStatsWithoutDuplicatesAndKeepsUserFlag() = runBlocking {
        val first = repo.saveDetected(result(candidate())).single()
        repo.setUserDisabled(first.id, true)

        // Той самий вечір, трохи інші межі й статистика.
        val again = repo.saveDetected(result(candidate(start = 22 * 60 + 10, end = 23 * 60 + 40, avg = 41, recurrence = 5)))

        assertEquals(1, again.size)
        val w = again.single()
        assertEquals(first.id, w.id)
        assertEquals(22 * 60, w.startMinute) // межі змінює лише людина
        assertEquals(41, w.avgMinutes)
        assertEquals(5, w.recurrenceDays)
        assertTrue(w.isUserDisabled)
    }

    @Test
    fun distinctWindowIsAddedAndOldOnesNeverRemoved() = runBlocking {
        repo.saveDetected(result(candidate()))
        val after = repo.saveDetected(result(candidate(start = 13 * 60, end = 13 * 60 + 30)))
        assertEquals(2, after.size)
        // Перерахунок без жодного вікна нічого не видаляє.
        assertEquals(2, repo.saveDetected(result()).size)
    }

    @Test
    fun notEnoughDataChangesNothing() = runBlocking {
        val before = repo.saveDetected(result(candidate()))
        val after = repo.saveDetected(result(candidate(start = 13 * 60, end = 13 * 60 + 30), enough = false))
        assertEquals(before.map { it.id }, after.map { it.id })
    }

    @Test
    fun onePlanPerWindowAndDeletedWithWindow() = runBlocking {
        val window = repo.saveDetected(result(candidate())).single()
        val p1 = repo.savePlan(window.id, " Якщо я лягаю в ліжко ", "відкладу телефон ")
        val p2 = repo.savePlan(window.id, "Якщо я лягаю", "читатиму")

        assertEquals(p1.id, p2.id)
        assertEquals(p1.createdAt, p2.createdAt)
        assertEquals("Якщо я лягаю в ліжко", p1.ifText)
        assertEquals("читатиму", repo.getPlan(window.id)?.thenText)

        db.openHelper.writableDatabase.execSQL("DELETE FROM risk_windows")
        assertNull(repo.getPlan(window.id))
        assertNotNull(p2)
    }
}
