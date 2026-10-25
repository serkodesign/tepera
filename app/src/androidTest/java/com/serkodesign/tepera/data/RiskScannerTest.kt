package com.serkodesign.tepera.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.repository.RiskScanner
import com.serkodesign.tepera.data.repository.RiskWindowRepository
import com.serkodesign.tepera.data.repository.ScanOutcome
import com.serkodesign.tepera.data.repository.ScanSnapshotStore
import com.serkodesign.tepera.data.repository.SlotRecompute
import com.serkodesign.tepera.data.repository.SlotSource
import com.serkodesign.tepera.domain.risk.DayType
import com.serkodesign.tepera.domain.risk.RiskCandidate
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.RiskResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Тижневий скан без реальних системних подій: слоти підмінені, БД у пам'яті, знімки — у змінній. */
@RunWith(AndroidJUnit4::class)
class RiskScannerTest {
    private lateinit var db: AppDatabase
    private lateinit var windows: RiskWindowRepository
    private lateinit var scanner: RiskScanner

    private var week = 0L
    private var candidates: List<RiskCandidate> = emptyList()
    private var enoughData = true
    private var snapshots = ""

    private val source = object : SlotSource {
        override suspend fun refresh(nowMillis: Long): SlotRecompute = SlotRecompute.Done(7, 0)
        override suspend fun findWindows(cfg: RiskConfig, nowMillis: Long): RiskResult {
            val last = LocalDate.of(2026, 10, 20).plusWeeks(week)
            return RiskResult((6 downTo 0).map { last.minusDays(it.toLong()) }, 7, enoughData, candidates)
        }
    }
    private val store = object : ScanSnapshotStore {
        override suspend fun load() = snapshots
        override suspend fun save(raw: String) {
            snapshots = raw
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        windows = RiskWindowRepository(db.riskWindowDao(), db.planDao(), db.usageSlotDao())
        scanner = RiskScanner(source, windows, store)
    }

    @After
    fun tearDown() = db.close()

    private fun window(start: Int, end: Int) = RiskCandidate(start, end, 6, 7, 36, emptyList(), DayType.ALL)

    private fun scanWeek(w: Long, vararg c: RiskCandidate): ScanOutcome {
        week = w
        candidates = c.toList()
        return runBlocking { scanner.scan() }
    }

    @Test
    fun startShiftedTwoWeeksInARowIsOnlySuggestedAndCanBeApplied() = runBlocking {
        scanWeek(0, window(22 * 60, 23 * 60 + 30))
        scanWeek(1, window(22 * 60 + 5, 23 * 60 + 35)) // ще в межах 20 хв
        val third = scanWeek(2, window(22 * 60 + 30, 0)) as ScanOutcome.Done
        // Два тижні поспіль зсув < 20 хв, потім ≥ 20: ще нема.
        assertEquals(0, third.shiftSuggestions)

        val fourth = scanWeek(3, window(22 * 60 + 40, 60)) as ScanOutcome.Done
        assertEquals(1, fourth.shiftSuggestions)
        val w = fourth.windows.single()
        assertEquals(22 * 60, w.startMinute) // межі не змінено мовчки
        assertEquals(22 * 60 + 40, w.suggestedStartMinute)

        windows.applyShift(w.id)
        val applied = windows.getAll().single()
        assertEquals(22 * 60 + 40, applied.startMinute)
        assertEquals(60, applied.endMinute)
        assertNull(applied.suggestedStartMinute)
    }

    @Test
    fun dismissedSuggestionDisappears() = runBlocking {
        scanWeek(0, window(22 * 60 + 30, 23 * 60 + 59))
        // Вікно створилось із зсунутими межами week 0; зсунемо відносно нього ще далі двічі.
        scanWeek(1, window(23 * 60, 0 + 30))
        val done = scanWeek(2, window(23 * 60 + 10, 40)) as ScanOutcome.Done
        assertEquals(1, done.shiftSuggestions)
        windows.dismissShift(done.windows.single().id)
        assertNull(windows.getAll().single().suggestedStartMinute)
    }

    @Test
    fun runsWithAMonthGapAreNotConsecutiveWeeks() = runBlocking {
        scanWeek(0, window(22 * 60, 23 * 60 + 30))
        scanWeek(4, window(22 * 60 + 30, 0))
        val done = scanWeek(8, window(22 * 60 + 40, 60)) as ScanOutcome.Done
        assertEquals(0, done.shiftSuggestions)
    }

    @Test
    fun userDisabledWindowGetsNoSuggestion() = runBlocking {
        val first = scanWeek(0, window(22 * 60, 23 * 60 + 30)) as ScanOutcome.Done
        windows.setUserDisabled(first.windows.single().id, true)
        scanWeek(1, window(22 * 60 + 30, 0))
        val done = scanWeek(2, window(22 * 60 + 40, 60)) as ScanOutcome.Done
        assertEquals(0, done.shiftSuggestions)
    }

    @Test
    fun notEnoughDataSavesNothing() = runBlocking {
        enoughData = false
        val outcome = scanWeek(0, window(22 * 60, 23 * 60 + 30))
        assertEquals(ScanOutcome.NotEnoughData(7), outcome)
        assertEquals(0, windows.getAll().size)
        assertEquals("", snapshots)
    }
}
