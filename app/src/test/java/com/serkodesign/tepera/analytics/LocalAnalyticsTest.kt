package com.serkodesign.tepera.analytics

import com.serkodesign.tepera.data.local.dao.DailyEventCounterDao
import com.serkodesign.tepera.data.local.entity.DailyEventCounterEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LocalAnalyticsTest {

    private class FakeDao : DailyEventCounterDao {
        val counts = mutableMapOf<Triple<LocalDate, String, String>, Int>()
        var fail = false
        override suspend fun insertIfAbsent(row: DailyEventCounterEntity): Long = error("unused")
        override suspend fun bump(date: LocalDate, eventName: String, propsKey: String): Int = error("unused")
        override suspend fun increment(date: LocalDate, eventName: String, propsKey: String) {
            if (fail) error("db down")
            counts.merge(Triple(date, eventName, propsKey), 1, Int::plus)
        }
        override suspend fun getRange(from: LocalDate, to: LocalDate) = emptyList<DailyEventCounterEntity>()
        override suspend fun deleteOlderThan(before: LocalDate): Int = 0
        override suspend fun deleteAll() = Unit
    }

    private val day = LocalDate.of(2026, 11, 26)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test
    fun propsKey_isSortedAndAllowlisted() {
        assertEquals("", analyticsPropsKey(emptyMap()))
        assertEquals("tab=data", analyticsPropsKey(AnalyticsEvent.TabViewed(Tab.DATA).props))
        assertEquals(
            "enabled=true;tab=data",
            analyticsPropsKey(mapOf("tab" to "data", "package_name" to "x.y", "enabled" to true))
        )
    }

    @Test
    fun track_incrementsCounterPerDayEventAndProps() {
        val dao = FakeDao()
        val analytics = LocalAnalytics(dao, scope) { day }
        analytics.track(AnalyticsEvent.ActivityAdded)
        analytics.track(AnalyticsEvent.ActivityAdded)
        analytics.track(AnalyticsEvent.TabViewed(Tab.TODAY))
        assertEquals(2, dao.counts[Triple(day, "activity_added", "")])
        assertEquals(1, dao.counts[Triple(day, "tab_viewed", "tab=today")])
    }

    @Test
    fun track_swallowsStorageErrors() {
        val dao = FakeDao().apply { fail = true }
        LocalAnalytics(dao, scope) { day }.track(AnalyticsEvent.ActivityAdded)
        assertEquals(0, dao.counts.size)
    }

    @Test
    fun retention_cutoffIs90DaysBack() {
        assertEquals(LocalDate.of(2026, 8, 28), analyticsRetentionCutoff(day))
    }
}
