package com.serkodesign.tepera.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.local.MIGRATION_17_18
import com.serkodesign.tepera.data.local.MIGRATION_18_19
import com.serkodesign.tepera.data.local.MIGRATION_19_20
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Міграції v3 MVP на окремій тестовій БД (реальна `tepera.db` застосунку не чіпається). БД старої версії
 * створюється зі збереженої Room-схеми `app/schemas/.../<версія>.json`, потім відкривається через Room з
 * міграціями: Room сам звіряє результат зі схемою поточної версії (таблиці, індекси, зовнішні ключі, hash) і
 * падає, якщо SQL міграції розходиться з сутностями.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    /** Створює порожню БД зазначеної версії за експортованою схемою. */
    private fun createOldDatabase(version: Int): SQLiteDatabase {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.serkodesign.tepera.data.local.AppDatabase/$version.json")
            .bufferedReader().use { JSONObject(it.readText()) }
            .getJSONObject("database")
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName), null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices")
            for (j in 0 until (indices?.length() ?: 0)) {
                db.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = version
        return db
    }

    private fun openMigrated(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
            .build()

    @Test
    fun migrate18to19_keepsExistingRowsAndCreatesNewTables() {
        createOldDatabase(18).apply {
            execSQL("INSERT INTO usage_slots (date, slot, packageName, seconds) VALUES (20750, 88, 'com.example.feed', 300)")
            close()
        }

        val room = openMigrated()
        val db = room.openHelper.writableDatabase // відкриття запускає міграцію й звірку схеми

        db.query("SELECT COUNT(*), SUM(seconds) FROM usage_slots").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
            assertEquals(300, it.getInt(1))
        }
        db.execSQL(
            "INSERT INTO risk_windows (id, startMinute, endMinute, dayType, recurrenceDays, observedDays, avgMinutes, " +
                "topApps, createdAt, updatedAt, isUserDisabled) VALUES ('w1', 1320, 1410, 'ALL', 6, 7, 36, '', 1, 1, 0)"
        )
        db.execSQL("INSERT INTO plans (id, windowId, ifText, thenText, createdAt, updatedAt) VALUES ('p1', 'w1', 'a', 'b', 1, 1)")
        db.execSQL("INSERT INTO nudge_events (id, windowId, scheduledAt, shownAt, action) VALUES ('n1', 'w1', 1, NULL, 'NONE')")
        db.execSQL("INSERT INTO feed_apps (packageName, label, confirmedAt) VALUES ('com.example.feed', 'Feed', 1)")
        db.execSQL("INSERT INTO observation_periods (id, startDate, endDate) VALUES ('o1', 20744, NULL)")

        // Каскад: видалення вікна прибирає план і нагадування.
        db.execSQL("DELETE FROM risk_windows WHERE id = 'w1'")
        db.query("SELECT (SELECT COUNT(*) FROM plans), (SELECT COUNT(*) FROM nudge_events)").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertEquals(0, it.getInt(1))
        }
        room.close()
    }


    @Test
    fun migrate19to20_addsEmptyShiftSuggestionColumns() {
        createOldDatabase(19).apply {
            execSQL(
                "INSERT INTO risk_windows (id, startMinute, endMinute, dayType, recurrenceDays, observedDays, avgMinutes, " +
                    "topApps, createdAt, updatedAt, isUserDisabled) VALUES ('w1', 1320, 1410, 'ALL', 6, 7, 36, '', 1, 1, 0)"
            )
            close()
        }
        val room = openMigrated()
        room.openHelper.writableDatabase.query(
            "SELECT startMinute, suggestedStartMinute, suggestedEndMinute FROM risk_windows WHERE id = 'w1'"
        ).use {
            it.moveToFirst()
            assertEquals(1320, it.getInt(0))
            assertTrue(it.isNull(1))
            assertTrue(it.isNull(2))
        }
        room.close()
    }
    @Test
    fun migrateFrom17ThroughAllV3Steps() {
        createOldDatabase(17).close()
        val room = openMigrated()
        room.openHelper.writableDatabase.query("SELECT COUNT(*) FROM feed_apps").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        room.close()
    }
}
