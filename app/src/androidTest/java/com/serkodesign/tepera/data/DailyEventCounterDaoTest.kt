package com.serkodesign.tepera.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.local.dao.DailyEventCounterDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DailyEventCounterDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: DailyEventCounterDao
    private val d1 = LocalDate.of(2026, 11, 1)
    private val d2 = LocalDate.of(2026, 11, 2)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.dailyEventCounterDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun increment_createsThenAccumulates() = runBlocking {
        repeat(3) { dao.increment(d1, "tab_viewed", "tab=data") }
        dao.increment(d1, "tab_viewed", "tab=today")
        dao.increment(d2, "tab_viewed", "tab=data")
        val rows = dao.getRange(d1, d2)
        assertEquals(3, rows.size)
        assertEquals(3, rows.first { it.date == d1 && it.propsKey == "tab=data" }.count)
        assertEquals(1, rows.first { it.date == d1 && it.propsKey == "tab=today" }.count)
    }

    @Test
    fun getRange_isInclusive() = runBlocking {
        dao.increment(d1, "a", "")
        dao.increment(d2, "a", "")
        assertEquals(1, dao.getRange(d2, d2).size)
        assertEquals(2, dao.getRange(d1, d2).size)
    }

    @Test
    fun deleteOlderThan_removesOnlyOlderDays() = runBlocking {
        dao.increment(d1, "a", "")
        dao.increment(d2, "a", "")
        dao.deleteOlderThan(d2)
        assertEquals(listOf(d2), dao.getRange(d1, d2).map { it.date })
    }

    @Test
    fun deleteAll_clearsTable() = runBlocking {
        dao.increment(d1, "a", "")
        dao.deleteAll()
        assertEquals(0, dao.getRange(d1, d2).size)
    }
}
