package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.cards.CardHistorySource
import com.serkodesign.tepera.data.cards.CardResult
import com.serkodesign.tepera.data.cards.CardType
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.dao.CardShowDao
import com.serkodesign.tepera.data.local.entity.CardShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * T-13 (tepera-dev-spec.md), продакшн-реалізація [CardHistorySource] — Room для самої історії
 * показів (`card_show_history`), `SettingsStore` для лічильника витіснень (простий скаляр,
 * той самий підхід, що `gatesPausedUntilMillis` — не потребує окремої таблиці).
 */
class CardHistoryRepository(
    private val dao: CardShowDao,
    private val settingsStore: SettingsStore
) : CardHistorySource {

    /** Не частіше разу на календарний день на тип — повторні кадри Home не спамлять таблицю. */
    suspend fun recordShown(type: CardType, atMillis: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            val latest = dao.getLatestAny(type.name)
            if (latest != null && isSameDay(latest.atMillis, atMillis)) return@withContext
            dao.insert(CardShowEntity(UUID.randomUUID().toString(), type.name, type.isEstimate, atMillis, CardResult.SHOWN.name))
        }

    suspend fun recordResolved(type: CardType, result: CardResult, atMillis: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            dao.insert(CardShowEntity(UUID.randomUUID().toString(), type.name, type.isEstimate, atMillis, result.name))
        }

    override suspend fun lastResolvedAt(type: CardType): Long? = withContext(Dispatchers.IO) {
        dao.getLastResolved(type.name)?.atMillis
    }

    override suspend fun estimateShownSince(sinceMillis: Long): Boolean = withContext(Dispatchers.IO) {
        dao.hasEstimateShownSince(sinceMillis)
    }

    override suspend fun eventDisplacementStreak(): Int = settingsStore.cardEventDisplacementStreak.first()

    override suspend fun setEventDisplacementStreak(value: Int) {
        settingsStore.setCardEventDisplacementStreak(value)
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }
}
