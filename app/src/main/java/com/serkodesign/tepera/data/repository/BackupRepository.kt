package com.serkodesign.tepera.data.repository

import androidx.room.withTransaction
import com.serkodesign.tepera.data.local.AppDatabase
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.local.entity.ExcludedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val SCHEMA_VERSION = 1

/**
 * FR-6.2: ручний JSON-експорт/імпорт — доповнення до Android Auto Backup (FR-6.1), яке працює
 * навіть без Google-акаунта на пристрої і дає користувачу файл, який він контролює сам.
 *
 * Імпорт ПОВНІСТЮ ЗАМІНЮЄ локальні дані (не зливає з наявними) — найпростіша та найпередбачуваніша
 * семантика для MVP. id категорій/записів з файлу зберігаються буквально (OnConflictStrategy.REPLACE),
 * тому файл має бути саме тим, що експортував сам застосунок, а не написаним вручну.
 */
class BackupRepository(
    private val database: AppDatabase,
    private val settingsStore: SettingsStore
) {

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val categories = database.categoryDao().getAllOnce()
        val entries = database.activityEntryDao().getAllOnce()
        val excludedApps = database.excludedAppDao().getAllOnce()
        val targetMinutes = settingsStore.targetMinutes.first()

        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", JSONObject().put("targetMinutes", targetMinutes))

        root.put("categories", JSONArray().apply {
            categories.forEach { put(categoryToJson(it)) }
        })
        root.put("activityEntries", JSONArray().apply {
            entries.forEach { put(entryToJson(it)) }
        })
        root.put("excludedApps", JSONArray().apply {
            excludedApps.forEach { put(excludedAppToJson(it)) }
        })

        root.toString(2)
    }

    /** @throws org.json.JSONException якщо файл не є коректним JSON-бекапом Tepera. */
    suspend fun importFromJson(json: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(json)

        val categories = root.getJSONArray("categories").let { array ->
            List(array.length()) { categoryFromJson(array.getJSONObject(it)) }
        }
        val entries = root.getJSONArray("activityEntries").let { array ->
            List(array.length()) { entryFromJson(array.getJSONObject(it)) }
        }
        val excludedApps = root.getJSONArray("excludedApps").let { array ->
            List(array.length()) { excludedAppFromJson(array.getJSONObject(it)) }
        }
        val targetMinutes = root.optJSONObject("settings")?.optInt("targetMinutes")

        database.withTransaction {
            database.activityEntryDao().deleteAll() // спершу дочірня таблиця (FK на categories)
            database.categoryDao().deleteAll()
            database.excludedAppDao().deleteAll()

            database.categoryDao().insertAll(categories)
            database.activityEntryDao().insertAll(entries)
            database.excludedAppDao().insertAll(excludedApps)
        }

        if (targetMinutes != null && targetMinutes > 0) {
            settingsStore.setTargetMinutes(targetMinutes)
        }
    }

    private fun categoryToJson(c: CategoryEntity) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("nameKey", c.nameKey ?: JSONObject.NULL)
        put("iconName", c.iconName)
        put("colorHex", c.colorHex)
        put("isDefault", c.isDefault)
        put("isCustom", c.isCustom)
        put("isAutoTracked", c.isAutoTracked)
        put("isHidden", c.isHidden)
        put("sortOrder", c.sortOrder)
    }

    private fun categoryFromJson(o: JSONObject) = CategoryEntity(
        id = o.getString("id"),
        name = o.getString("name"),
        nameKey = if (o.isNull("nameKey")) null else o.getString("nameKey"),
        iconName = o.getString("iconName"),
        colorHex = o.getString("colorHex"),
        isDefault = o.getBoolean("isDefault"),
        isCustom = o.getBoolean("isCustom"),
        isAutoTracked = o.getBoolean("isAutoTracked"),
        isHidden = o.getBoolean("isHidden"),
        sortOrder = o.getInt("sortOrder")
    )

    private fun entryToJson(e: ActivityEntryEntity) = JSONObject().apply {
        put("id", e.id)
        put("categoryId", e.categoryId)
        put("startTime", e.startTime)
        put("durationMinutes", e.durationMinutes)
        put("note", e.note ?: JSONObject.NULL)
        put("source", e.source.name)
        put("createdAt", e.createdAt)
    }

    private fun entryFromJson(o: JSONObject) = ActivityEntryEntity(
        id = o.getString("id"),
        categoryId = o.getString("categoryId"),
        startTime = o.getLong("startTime"),
        durationMinutes = o.getInt("durationMinutes"),
        note = if (o.isNull("note")) null else o.getString("note"),
        // Форвард-сумісність: невідоме джерело з майбутнього формату не має ламати імпорт.
        source = runCatching { EntrySource.valueOf(o.getString("source")) }.getOrDefault(EntrySource.MANUAL),
        createdAt = o.getLong("createdAt")
    )

    private fun excludedAppToJson(a: ExcludedAppEntity) = JSONObject().apply {
        put("packageName", a.packageName)
        put("cachedLabel", a.cachedLabel)
        put("addedAt", a.addedAt)
    }

    private fun excludedAppFromJson(o: JSONObject) = ExcludedAppEntity(
        packageName = o.getString("packageName"),
        cachedLabel = o.getString("cachedLabel"),
        addedAt = o.getLong("addedAt")
    )
}
