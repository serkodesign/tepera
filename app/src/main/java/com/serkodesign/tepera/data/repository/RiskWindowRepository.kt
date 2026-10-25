package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.PlanDao
import com.serkodesign.tepera.data.local.dao.RiskWindowDao
import com.serkodesign.tepera.data.local.dao.UsageSlotDao
import com.serkodesign.tepera.data.local.entity.PlanEntity
import com.serkodesign.tepera.data.local.entity.RiskWindowEntity
import com.serkodesign.tepera.domain.risk.RiskCandidate
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.RiskResult
import com.serkodesign.tepera.domain.risk.RiskWindowFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * v3.0 MVP, Етап 1: збереження годин ризику й планів. Перерахунок ([saveDetected]) НІКОЛИ не видаляє вікна й плани
 * (дані людини) і не повертає вимкнене вікно: оновлює лише статистику вікна, яке вже відоме, а нові додає.
 * Межі відомого вікна змінює тільки сама людина (пропозиція зсуву, Етап 5).
 */
class RiskWindowRepository(
    private val windowDao: RiskWindowDao,
    private val planDao: PlanDao,
    private val slotDao: UsageSlotDao
) {
    fun observeWindows(): Flow<List<RiskWindowEntity>> = windowDao.observeAll()

    fun observePlans(): Flow<List<PlanEntity>> = planDao.observeAll()

    suspend fun getPlan(windowId: String): PlanEntity? = withContext(Dispatchers.IO) { planDao.getForWindow(windowId) }

    suspend fun getWindow(windowId: String): RiskWindowEntity? = withContext(Dispatchers.IO) { windowDao.getById(windowId) }

    suspend fun getAll(): List<RiskWindowEntity> = withContext(Dispatchers.IO) { windowDao.getAll() }

    suspend fun setShiftSuggestion(windowId: String, start: Int?, end: Int?) = withContext(Dispatchers.IO) {
        windowDao.setShiftSuggestion(windowId, start, end)
    }

    /** Людина підтвердила зсув: межі вікна стають запропонованими. */
    suspend fun applyShift(windowId: String) = withContext(Dispatchers.IO) {
        windowDao.applyShiftSuggestion(windowId, System.currentTimeMillis())
    }

    /** Людина відмовилась: пропозиція зникає (з'явиться знову, лише якщо зсув триватиме й надалі). */
    suspend fun dismissShift(windowId: String) = withContext(Dispatchers.IO) {
        windowDao.setShiftSuggestion(windowId, null, null)
    }

    suspend fun setUserDisabled(windowId: String, disabled: Boolean) = withContext(Dispatchers.IO) {
        windowDao.setUserDisabled(windowId, disabled, System.currentTimeMillis())
    }

    /** Зберігає план словами людини; повторне збереження оновлює той самий план, зберігаючи `createdAt`. */
    suspend fun savePlan(windowId: String, ifText: String, thenText: String): PlanEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = planDao.getForWindow(windowId)
        val plan = PlanEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            windowId = windowId,
            ifText = ifText.trim(),
            thenText = thenText.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        planDao.upsert(plan)
        plan
    }

    /** Зберігає результат пошуку вікон; повертає актуальний стан усіх вікон. */
    suspend fun saveDetected(
        result: RiskResult,
        cfg: RiskConfig = RiskConfig(),
        nowMillis: Long = System.currentTimeMillis()
    ): List<RiskWindowEntity> = withContext(Dispatchers.IO) {
        if (!result.enoughData) return@withContext windowDao.getAll()
        val existing = windowDao.getAll()
        val used = HashSet<String>()
        for (c in result.candidates) {
            val topApps = topAppsIn(c, result, cfg)
            val match = existing.firstOrNull {
                it.id !in used && RiskWindowFinder.sameWindow(it.startMinute, it.endMinute, c.startMinute, c.endMinute, cfg)
            }
            if (match != null) {
                used += match.id
                windowDao.update(
                    match.copy(
                        dayType = c.dayType,
                        recurrenceDays = c.recurrenceDays,
                        observedDays = c.observedDays,
                        avgMinutes = c.avgMinutes,
                        topApps = topApps,
                        updatedAt = nowMillis
                    )
                )
            } else {
                windowDao.insert(
                    RiskWindowEntity(
                        id = UUID.randomUUID().toString(),
                        startMinute = c.startMinute,
                        endMinute = c.endMinute,
                        dayType = c.dayType,
                        recurrenceDays = c.recurrenceDays,
                        observedDays = c.observedDays,
                        avgMinutes = c.avgMinutes,
                        topApps = topApps,
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        isUserDisabled = false
                    )
                )
            }
        }
        windowDao.getAll()
    }

    /** До трьох пакетів з найбільшим часом усередині вікна за логічні доби спостереження. */
    private suspend fun topAppsIn(c: RiskCandidate, result: RiskResult, cfg: RiskConfig): String {
        if (result.days.isEmpty()) return ""
        val days = result.days.toSet()
        val firstSlot = RiskWindowFinder.startPosition(c.startMinute, cfg) / RiskWindowFinder.SLOT_MINUTES
        val slotCount = (RiskWindowFinder.lengthMinutes(c.startMinute, c.endMinute) + RiskWindowFinder.SLOT_MINUTES - 1) /
            RiskWindowFinder.SLOT_MINUTES
        val rows = slotDao.getRange(result.days.first().toEpochDay(), result.days.last().plusDays(1).toEpochDay())
        return rows
            .filter { row ->
                val date = java.time.LocalDate.ofEpochDay(row.date)
                RiskWindowFinder.logicalDate(date, row.slot, cfg) in days &&
                    RiskWindowFinder.position(row.slot, cfg) in firstSlot until firstSlot + slotCount
            }
            .groupBy { it.packageName }
            .mapValues { (_, v) -> v.sumOf { it.seconds } }
            .entries.sortedByDescending { it.value }
            .take(3)
            .joinToString(",") { it.key }
    }
}
