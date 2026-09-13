package com.serkodesign.tepera.debug

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SpikeT1"
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

data class SpikeT1UiState(
    val running: Boolean = false,
    val visibilityResult: String = "",
    val historyDepthResult: String = "",
    val pinShortcutResult: String = "",
    val launchResult: String = "",
    val keyguardResult: String = ""
)

/**
 * T-1 (tepera-dev-spec.md) — СПАЙК: видимість пакетів і закріплені ярлики. Debug-only
 * інструмент (не продакшн-фіча), доступ лише з debuggable збірки — див. умову в
 * `SettingsScreen.kt`. Мета не в коді, а в таблиці "4 пристрої × 5 пунктів" (розділ 0.3
 * ТЗ вимагає внести результати спайку в документ до старту T-4/T-5), інструмент лише
 * механізує збір даних, щоб не повторювати ручні adb-команди на кожному з 4 пристроїв.
 */
class SpikeT1ViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(SpikeT1UiState())
    val uiState: StateFlow<SpikeT1UiState> = _uiState.asStateFlow()

    /** Пункт 1 + 2 разом: обидва читають той самий queryEvents(), нема сенсу питати двічі. */
    fun runPackageVisibilityAndHistoryDepth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(running = true)
            val (visibility, depth) = withContext(Dispatchers.IO) {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val now = System.currentTimeMillis()
                // 0L як start — офіційний спосіб дістати всю збережену системою історію,
                // не лише довільне вікно; саме так вимірюється "глибина" в пункті 2.
                val events = try {
                    usm.queryEvents(0L, now)
                } catch (e: SecurityException) {
                    null
                }
                if (events == null) {
                    "Немає доступу до UsageStatsManager (дозвіл не наданий)." to "—"
                } else {
                    val seenPackages = LinkedHashSet<String>()
                    var earliest = now
                    val event = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        seenPackages.add(event.packageName)
                        if (event.timeStamp < earliest) earliest = event.timeStamp
                    }
                    val pm = context.packageManager
                    val notLaunchable = seenPackages.filter { pm.getLaunchIntentForPackage(it) == null }
                    val notResolvableInfo = seenPackages.filter {
                        try {
                            pm.getApplicationInfo(it, 0)
                            false
                        } catch (e: PackageManager.NameNotFoundException) {
                            true
                        }
                    }
                    val visibilityText = buildString {
                        appendLine("Пакетів у подіях: ${seenPackages.size}")
                        appendLine("Без getLaunchIntentForPackage(): ${notLaunchable.size}")
                        if (notLaunchable.isNotEmpty()) {
                            appendLine("  " + notLaunchable.take(10).joinToString(", "))
                        }
                        appendLine("Без getApplicationInfo() (NameNotFoundException): ${notResolvableInfo.size}")
                        if (notResolvableInfo.isNotEmpty()) {
                            appendLine("  " + notResolvableInfo.take(10).joinToString(", "))
                        }
                    }.trim()

                    val depthDays = (now - earliest) / DAY_MILLIS
                    val depthText = "Найдавніша подія: ${depthDays} дн. тому " +
                        "(${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(earliest))})"

                    visibilityText to depthText
                }
            }
            Log.d(TAG, "visibility=$visibility depth=$depth")
            _uiState.value = _uiState.value.copy(
                running = false,
                visibilityResult = visibility,
                historyDepthResult = depth
            )
        }
    }

    /**
     * Пункт 3: чи підтримується requestPinShortcut() і чи справді з'являється ярлик на
     * стоковому лаунчері. Ціль ярлика — сам Tepera (MainActivity) — спайк перевіряє механіку
     * закріплення на конкретному лаунчері, не саму фічу воріт (та ще не існує, T-4).
     * Візуальне підтвердження ("ярлик дійсно з'явився на робочому столі") робить користувач
     * особисто на кожному пристрої — це не автоматизується.
     */
    fun testPinShortcut(targetPackage: String) {
        val supported = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
        if (!supported) {
            _uiState.value = _uiState.value.copy(
                pinShortcutResult = "isRequestPinShortcutSupported() = false — не підтримується на цьому лаунчері/OS."
            )
            return
        }
        val label = if (targetPackage.isBlank()) "Tepera Spike" else "Spike: $targetPackage"
        val shortcut = ShortcutInfoCompat.Builder(context, "spike_t1_${System.currentTimeMillis()}")
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(context, com.serkodesign.tepera.R.mipmap.ic_launcher))
            .setIntent(
                Intent(context, com.serkodesign.tepera.MainActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
            )
            .build()
        val requested = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        _uiState.value = _uiState.value.copy(
            pinShortcutResult = "isRequestPinShortcutSupported() = true. requestPinShortcut() надіслано: $requested. " +
                "Перевір вручну, чи з'явився ярлик на робочому столі."
        )
    }

    /** Пункт 4: чи запускається цільовий застосунок через getLaunchIntentForPackage(). */
    fun testLaunch(targetPackage: String) {
        if (targetPackage.isBlank()) {
            _uiState.value = _uiState.value.copy(launchResult = "Вкажи package name цільового застосунку вище.")
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(targetPackage)
        if (intent == null) {
            _uiState.value = _uiState.value.copy(launchResult = "getLaunchIntentForPackage(\"$targetPackage\") = null.")
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        _uiState.value = _uiState.value.copy(launchResult = "Запущено \"$targetPackage\" — якщо відкрився застосунок, працює.")
    }

    /**
     * Пункт 5: чи доступний UsageEvents.Event.KEYGUARD_HIDDEN (API 28+, T-14 уже спирається на
     * нього) і чи щось наближене трапляється нижче 28. Порядок: натиснути кнопку, тоді кілька
     * разів заблокувати/розблокувати екран, тоді натиснути ще раз — друге натискання рахує
     * нові події за вікно з першого натискання.
     */
    private var keyguardWindowStart: Long = -1L

    fun checkKeyguardEvents() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (keyguardWindowStart < 0) {
                keyguardWindowStart = now
                _uiState.value = _uiState.value.copy(
                    keyguardResult = "Вікно почалось о " +
                        "${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(now))}. " +
                        "Заблокуй/розблокуй екран кілька разів, тоді натисни ще раз."
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val events = usm.queryEvents(keyguardWindowStart, now)
                val event = UsageEvents.Event()
                var keyguardHiddenCount = 0
                var keyguardShownCount = 0
                val allTypes = mutableMapOf<Int, Int>()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    allTypes[event.eventType] = (allTypes[event.eventType] ?: 0) + 1
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) keyguardHiddenCount++
                        if (event.eventType == UsageEvents.Event.KEYGUARD_SHOWN) keyguardShownCount++
                    } else {
                        // Тип 18/19 не має константи в SDK нижче 28, але значення events самі —
                        // системний факт, не залежить від compileSdk. Рахуємо сирим числом.
                        if (event.eventType == 18) keyguardHiddenCount++
                        if (event.eventType == 19) keyguardShownCount++
                    }
                }
                "SDK_INT=${Build.VERSION.SDK_INT}. KEYGUARD_HIDDEN(18): $keyguardHiddenCount, " +
                    "KEYGUARD_SHOWN(19): $keyguardShownCount.\nУсі типи подій за вікно: $allTypes"
            }
            Log.d(TAG, "keyguard=$result")
            keyguardWindowStart = -1L
            _uiState.value = _uiState.value.copy(keyguardResult = result)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SpikeT1ViewModel(context.applicationContext) as T
    }
}
