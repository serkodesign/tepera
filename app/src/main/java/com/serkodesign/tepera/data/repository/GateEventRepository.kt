package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.GateEventDao
import com.serkodesign.tepera.data.local.entity.GateEventEntity
import com.serkodesign.tepera.data.local.entity.GateEventResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/** T-6 (tepera-dev-spec.md): запис і агрегація подій воріт — годує картку FR-P.3. */
class GateEventRepository(private val dao: GateEventDao) {

    suspend fun record(packageName: String, result: GateEventResult, atMillis: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            dao.insert(GateEventEntity(UUID.randomUUID().toString(), packageName, result.name, atMillis))
        }

    /**
     * "Цього місяця" — КАЛЕНДАРНИЙ місяць (буквальне формулювання документа), не ковзне 30-денне
     * вікно: 1-го числа лічильник видимо й навмисно скидається на нуль, а не поступово спадає.
     */
    suspend fun countCancelledThisMonth(now: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis
        dao.countByResultInRange(GateEventResult.CANCELLED.name, monthStart, monthEnd)
    }
}
