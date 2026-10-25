package com.serkodesign.tepera.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.serkodesign.tepera.data.local.dao.FeedAppDao
import com.serkodesign.tepera.data.local.entity.FeedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * v3.0 MVP, Етап 1: список застосунків-стрічок, підтверджений людиною. Єдине джерело правди для
 * [UsageSlotRepository] і майбутнього екрана «Вибір стрічок» (Етап 3).
 */
class FeedAppRepository(private val context: Context, private val dao: FeedAppDao) {

    fun observeAll(): Flow<List<FeedAppEntity>> = dao.observeAll()

    suspend fun packages(): Set<String> = withContext(Dispatchers.IO) { dao.getPackages().toSet() }

    suspend fun setConfirmed(packageName: String, label: String, confirmed: Boolean) = withContext(Dispatchers.IO) {
        if (confirmed) dao.insert(FeedAppEntity(packageName, label, System.currentTimeMillis())) else dao.delete(packageName)
    }

    /**
     * Етап 0 зберігав вибір у DataStore ([com.serkodesign.tepera.data.local.SettingsStore.feedPackages]) — переносить
     * його в таблицю один раз, поки вона порожня. Нічого не вмикає сам.
     */
    suspend fun importLegacyIfEmpty(legacyPackages: Set<String>) = withContext(Dispatchers.IO) {
        if (legacyPackages.isEmpty() || dao.count() > 0) return@withContext
        val now = System.currentTimeMillis()
        legacyPackages.forEach { dao.insert(FeedAppEntity(it, labelOf(it), now)) }
    }

    private fun labelOf(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}
