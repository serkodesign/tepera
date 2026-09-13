package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.SleepWindowDao
import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

// SRS "вікно сну (за замовчуванням 00:00–06:00)" — той самий дефолт, що мав попередній
// однозначний sleepWindowEndHour (=6), перенесений у явне вікно slot 1.
private const val DEFAULT_START_MINUTE = 0
private const val DEFAULT_END_MINUTE = 6 * 60

/**
 * T-12 (tepera-dev-spec.md): CRUD над `sleep_windows` (2 слоти — друге вікно для плаваючого
 * графіка, вимкнене за замовчуванням) — ViewModel ніколи не торкається SleepWindowDao напряму
 * (CLAUDE.md, "Архітектура").
 */
class SleepWindowRepository(private val dao: SleepWindowDao) {

    fun observeWindows(): Flow<List<SleepWindowEntity>> = dao.observeAll()

    suspend fun getWindows(): List<SleepWindowEntity> = dao.getAll()

    /** Лише ввімкнені вікна — те, що фактично впливає на розрахунок (BalanceRepository, PauseRepository). */
    suspend fun getEnabledWindows(): List<SleepWindowEntity> = dao.getAll().filter { it.enabled }

    suspend fun setWindow(slot: Int, startMinuteOfDay: Int, endMinuteOfDay: Int, enabled: Boolean) {
        dao.upsert(SleepWindowEntity(slot, startMinuteOfDay, endMinuteOfDay, enabled))
    }

    /** Ідемпотентно — безпечно викликати щозапуску (TeperaApp.onCreate(), той самий підхід, що ensureDefaultsSeeded()). */
    suspend fun seedDefaultsIfUnset() {
        if (dao.observeAll().first().isEmpty()) {
            dao.upsert(SleepWindowEntity(slot = 1, startMinuteOfDay = DEFAULT_START_MINUTE, endMinuteOfDay = DEFAULT_END_MINUTE, enabled = true))
            dao.upsert(SleepWindowEntity(slot = 2, startMinuteOfDay = DEFAULT_START_MINUTE, endMinuteOfDay = DEFAULT_END_MINUTE, enabled = false))
        }
    }
}
