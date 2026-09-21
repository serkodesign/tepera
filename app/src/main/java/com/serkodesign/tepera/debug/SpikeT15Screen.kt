package com.serkodesign.tepera.debug

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * T-15 (tepera-dev-spec.md) — debug-only спайк імпульсивних мікровзаємодій: одноразовий екран, що висипає сирі
 * UsageEvents (екран/блокування/передній-задній план) за останні 14 днів у CSV. Аналіз гіпотез H1-H4 робиться
 * ПОЗА застосунком (див. docs/spike-t15-results.md). Видно лише при FLAG_DEBUGGABLE (рядок у SettingsScreen);
 * у продакшн-логіку не входить і жодних метрик у UI не додає.
 *
 * CSV: timestampMillis,eventType,packageName. Копія лишається в filesDir/spike_t15_events.csv (для витягу через
 * adb run-as), а кнопка "Зберегти як…" віддає той самий файл через системний вибір місця.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpikeT15Screen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ще не вивантажено") }
    var lastFile by remember { mutableStateOf<File?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val file = lastFile
        if (uri == null || file == null) return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        status = "Збережено: " + uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("T-15: сирі події (CSV)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Вивантажує UsageEvents за 14 днів (система зазвичай тримає 7-10) у CSV для аналізу гіпотез H1-H4.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                scope.launch {
                    status = "Вивантажую…"
                    val result = withContext(Dispatchers.IO) { dumpEvents(context) }
                    lastFile = result.first
                    status = "Подій: ${result.second}, файл: ${result.first.absolutePath}"
                }
            }) { Text("Вивантажити CSV") }
            Button(
                enabled = lastFile != null,
                onClick = { saveLauncher.launch("tepera-t15-events.csv") }
            ) { Text("Зберегти як…") }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val TRACKED_EVENTS = mapOf(
    UsageEvents.Event.MOVE_TO_FOREGROUND to "FG",
    UsageEvents.Event.MOVE_TO_BACKGROUND to "BG",
    UsageEvents.Event.SCREEN_INTERACTIVE to "SCREEN_ON",
    UsageEvents.Event.SCREEN_NON_INTERACTIVE to "SCREEN_OFF",
    UsageEvents.Event.KEYGUARD_SHOWN to "KEYGUARD_SHOWN",
    UsageEvents.Event.KEYGUARD_HIDDEN to "KEYGUARD_HIDDEN"
)

private fun dumpEvents(context: Context): Pair<File, Int> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()
    val events = usm.queryEvents(now - 14L * 24 * 60 * 60 * 1000, now)
    val file = File(context.filesDir, "spike_t15_events.csv")
    var count = 0
    file.bufferedWriter().use { w ->
        w.appendLine("timestampMillis,eventType,packageName")
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val name = TRACKED_EVENTS[e.eventType] ?: continue
            w.appendLine("${e.timeStamp},$name,${e.packageName ?: ""}")
            count++
        }
    }
    return file to count
}
