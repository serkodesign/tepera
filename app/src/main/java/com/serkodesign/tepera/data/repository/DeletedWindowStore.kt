package com.serkodesign.tepera.data.repository

import android.content.Context

/**
 * Межі вікон, які людина видалила сама. Автоматичне вікно, видалене з бази, знайшлося б наступним скануванням знову,
 * тож перед видаленням його межі записуються сюди, і [RiskWindowRepository.saveDetected] такого кандидата пропускає.
 * SharedPreferences, а не Room: це лише список «не пропонуй знову», без міграції схеми.
 */
class DeletedWindowStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("deleted_windows", Context.MODE_PRIVATE)

    fun add(startMinute: Int, endMinute: Int) {
        runCatching { prefs.edit().putStringSet(KEY, all() + "$startMinute:$endMinute").apply() }
    }

    /** Пари «початок — кінець» (хвилини доби) усіх видалених вікон. */
    fun bounds(): List<Pair<Int, Int>> = all().mapNotNull { s ->
        val p = s.split(":")
        val a = p.getOrNull(0)?.toIntOrNull()
        val b = p.getOrNull(1)?.toIntOrNull()
        if (a != null && b != null) a to b else null
    }

    private fun all(): Set<String> = runCatching { prefs.getStringSet(KEY, emptySet()).orEmpty().toSet() }.getOrDefault(emptySet())

    private companion object {
        const val KEY = "bounds"
    }
}
