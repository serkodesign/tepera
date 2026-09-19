package com.serkodesign.tepera.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import com.serkodesign.tepera.data.TimerCheckWorker
import com.serkodesign.tepera.widget.TeperaWidget
import com.serkodesign.tepera.widget.TeperaWidget4x2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.activeTimersDataStore by preferencesDataStore(name = "active_timers")
private val ACTIVE_TIMERS_KEY = stringPreferencesKey("active_timers_json")

/**
 * Живий таймер "натиснув категорію на Home — почалось логування, натиснув ще раз — зупинилось".
 * Кілька категорій можуть таймитись одночасно (узгоджується з FR-1.3: різні категорії законно
 * перекриваються, напр. аудіокнига під час прогулянки — CLAUDE.md). DataStore, не Room: це
 * ефемерний runtime-стан сесії логування, не потребує Room-міграції заради тимчасової мапи
 * categoryId -> startTime, яка переживає лише смерть процесу, не потребує запитів/індексів.
 */
class ActiveTimerStore(private val context: Context) {

    val activeTimers: Flow<Map<String, Long>> = context.activeTimersDataStore.data.map { prefs ->
        val json = prefs[ACTIVE_TIMERS_KEY] ?: "{}"
        val obj = JSONObject(json)
        obj.keys().asSequence().associateWith { obj.getLong(it) }
    }

    suspend fun start(categoryId: String, startTime: Long = System.currentTimeMillis()) {
        context.activeTimersDataStore.edit { prefs ->
            val obj = JSONObject(prefs[ACTIVE_TIMERS_KEY] ?: "{}")
            obj.put(categoryId, startTime)
            prefs[ACTIVE_TIMERS_KEY] = obj.toString()
        }
        // Сповіщення "усе ще цим займаєшся?" через 4 год (за прямим рішенням користувача — лише
        // сповіщення, БЕЗ автозупинки самого таймера). ActiveTimerStore — природне місце для
        // цього виклику: єдине джерело істини для стану таймера, і вже тримає Context.
        TimerCheckWorker.schedule(context, categoryId, startTime)
    }

    /** @return час старту, який щойно зупинили, або null якщо для цієї категорії таймер не йшов. */
    suspend fun stop(categoryId: String): Long? {
        var startTime: Long? = null
        context.activeTimersDataStore.edit { prefs ->
            val obj = JSONObject(prefs[ACTIVE_TIMERS_KEY] ?: "{}")
            if (obj.has(categoryId)) {
                startTime = obj.getLong(categoryId)
                obj.remove(categoryId)
                prefs[ACTIVE_TIMERS_KEY] = obj.toString()
            }
        }
        // Ручна зупинка (тап по картці/віджету чи "Ні" в сповіщенні) — заплановану перевірку
        // більше не потрібно виконувати.
        TimerCheckWorker.cancel(context, categoryId)
        return startTime
    }

    /**
     * Просить обидва віджети (4x1 і 4x2) перемалюватись/створити сесію — викликається наприкінці
     * [com.serkodesign.tepera.data.toggleCategoryTimer], тож охоплює і Home, і кнопки віджета, і "Ні"
     * у сповіщенні. Сам стан живих сесій оновлюється потоками (WidgetLiveData) без цього виклику.
     */
    suspend fun refreshWidgets() {
        TeperaWidget().updateAll(context)
        TeperaWidget4x2().updateAll(context)
    }
}
