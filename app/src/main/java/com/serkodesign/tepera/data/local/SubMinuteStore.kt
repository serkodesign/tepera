package com.serkodesign.tepera.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.subMinuteDataStore by preferencesDataStore(name = "sub_minute")

/** Ключ доби у вигляді `yyyyMMdd` (локальна доба) — впорядковується як число. */
fun dayKeyOf(millis: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.YEAR) * 10_000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
}

/**
 * Секунди, що не склали повну хвилину: таймер коротший за 1 хв та "хвіст" довшого (2:40 → 2 хв у записі,
 * 40 с — сюди). Раніше вони губились. Лічильник на пару (доба, категорія) — за прямим запитом користувача
 * вони враховуються в даних доби, але в Статистиці показуються, лише коли сума категорії сягне 5 хв
 * (інакше "<5 хв"). DataStore, не Room: без міграції схеми; не потрапляє в експорт/імпорт JSON.
 * Ключ — "yyyyMMdd|categoryId".
 */
class SubMinuteStore(private val context: Context) {

    suspend fun clearAll() {
        context.subMinuteDataStore.edit { it.clear() }
    }

    private fun key(dayKey: Int, categoryId: String) = intPreferencesKey("$dayKey|$categoryId")

    /** Додає [seconds] (0..59) до лічильника доби, якій належить [atMillis], для [categoryId]. */
    suspend fun add(atMillis: Long, categoryId: String, seconds: Int) {
        if (seconds <= 0) return
        val k = key(dayKeyOf(atMillis), categoryId)
        context.subMinuteDataStore.edit { prefs -> prefs[k] = (prefs[k] ?: 0) + seconds }
    }

    /** Сума секунд по категоріях за доби від дня [fromMillis] до дня [toMillis] включно. */
    fun observeSecondsByCategory(fromMillis: Long, toMillis: Long): Flow<Map<String, Int>> {
        val from = dayKeyOf(fromMillis)
        val to = dayKeyOf(toMillis)
        return context.subMinuteDataStore.data.map { prefs -> aggregate(prefs, from, to) }
    }

    private fun aggregate(prefs: Preferences, fromDayKey: Int, toDayKey: Int): Map<String, Int> {
        val result = HashMap<String, Int>()
        for ((k, value) in prefs.asMap()) {
            val name = k.name
            val sep = name.indexOf('|')
            if (sep <= 0) continue
            val day = name.substring(0, sep).toIntOrNull() ?: continue
            if (day < fromDayKey || day > toDayKey) continue
            val categoryId = name.substring(sep + 1)
            result[categoryId] = (result[categoryId] ?: 0) + (value as? Int ?: 0)
        }
        return result
    }
}
