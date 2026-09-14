package com.serkodesign.tepera.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.data.local.SettingsStore
import com.serkodesign.tepera.data.local.dao.AppGateDao
import com.serkodesign.tepera.data.local.entity.AppGateEntity
import com.serkodesign.tepera.util.buildGateShortcutIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * T-4 (tepera-dev-spec.md, FR-G частина 1): застосунки з паузою перед запуском. Створення воріт
 * = закріплений ярлик (`requestPinShortcut()`) з бейджованою іконкою (`buildGateShortcutIcon`) +
 * рядок у `app_gates`. Сам екран паузи (те, що показується при тапі по ярлику) — T-5, окрема
 * сесія; ця точка входу поки веде в `MainActivity` із міткою пакета в extras, готовою для T-5.
 */
class GateRepository(
    private val context: Context,
    private val dao: AppGateDao,
    private val settingsStore: SettingsStore
) {
    val gates: Flow<List<AppGateEntity>> = dao.observeAll()

    fun isPinShortcutSupported(): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /**
     * @return true, якщо ворота реально створено (закріплено новий ярлик АБО повернуто раніше
     * вимкнений — див. нижче); false, якщо `requestPinShortcut()` не підтримується.
     *
     * **Реальний краш, знайдений і виправлений живим тестуванням (Samsung S23):** `shortcutIdFor()`
     * — детермінований ID (`gate_$packageName`), тож повторне створення воріт для застосунку, чиї
     * попередні ворота видаляли (`removeGate()` нижче лише ВИМИКАЄ ярлик — Android не дає
     * застосунку самостійно зняти закріплений ярлик), намагається запросити пін для ID, який уже
     * існує в системі, лише вимкненим. `ShortcutManager.requestPinShortcut()` для такого ID кидає
     * `IllegalArgumentException` ("already exists but disabled") аж до краху застосунку — без
     * жодного власного try/catch тут це валило весь процес (відтворено: створення воріт для
     * Instagram, чиї ворота існували й були видалені в попередній сесії T-4/T-5, крашило Tepera
     * щоразу). Фікс: якщо ярлик із таким ID уже закріплений, але вимкнений — не пінити заново
     * (сам факт закріплення не зникає з видаленням рядка в `app_gates`), а ввімкнути й оновити
     * наявний. Виклик `requestPinShortcut()` для решти випадків лишається обгорнутим у try/catch
     * як запобіжник від інших системних відмов того самого роду.
     */
    suspend fun createGate(packageName: String, label: String, delaySeconds: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (!isPinShortcutSupported()) return@withContext false

            val id = shortcutIdFor(packageName)
            val shortcut = ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(label)
                .setIcon(buildGateShortcutIcon(context, packageName))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(GATE_TARGET_PACKAGE_EXTRA, packageName)
                )
                .build()

            val requested = try {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                val existingDisabled = shortcutManager?.pinnedShortcuts
                    ?.any { it.id == id && !it.isEnabled } == true
                if (existingDisabled && shortcutManager != null) {
                    shortcutManager.enableShortcuts(listOf(id))
                    shortcutManager.updateShortcuts(listOf(shortcut.toShortcutInfo()))
                    true
                } else {
                    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                }
            } catch (e: Exception) {
                false
            }
            if (requested) {
                dao.insert(
                    AppGateEntity(
                        packageName = packageName,
                        delaySeconds = delaySeconds,
                        originalIconHandled = false
                    )
                )
            }
            requested
        }

    /**
     * Android не дає застосунку самостійно зняти вже закріплений ярлик з робочого столу —
     * `disableShortcuts()` (доступний напряму з API 25, minSdk тут 26) лишає його видимим, але
     * тап по ньому більше нічого не відкриває (ShortcutManager сам показує системне пояснення).
     */
    suspend fun removeGate(packageName: String) = withContext(Dispatchers.IO) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        shortcutManager?.disableShortcuts(listOf(shortcutIdFor(packageName)))
        dao.deleteByPackageName(packageName)
    }

    suspend fun markOriginalIconHandled(packageName: String) {
        dao.markOriginalIconHandled(packageName)
    }

    /**
     * За прямим запитом користувача: якщо людина видаляє ворітний ярлик з робочого столу
     * (long-press → "Вилучити"), відповідний рядок має зникнути й зі списку в застосунку — без
     * цього `app_gates` і реальний стан лаунчера розходились би назавжди (Android не дає
     * колбек на видалення pinned shortcut, спільного для всіх лаунчерів — доступний лише
     * `ShortcutManager.getPinnedShortcuts()`, on-demand опитування, той самий підхід, що
     * сканування пауз/патерну в цьому проєкті, не фонова задача). Викликається при кожному
     * відкритті `GatesScreen` (`GatesViewModel.init`).
     */
    suspend fun pruneRemovedShortcuts() = withContext(Dispatchers.IO) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return@withContext
        val pinnedIds = shortcutManager.pinnedShortcuts.map { it.id }.toSet()
        dao.getAllOnce().forEach { gate ->
            if (shortcutIdFor(gate.packageName) !in pinnedIds) {
                dao.deleteByPackageName(gate.packageName)
            }
        }
    }

    /**
     * T-5 (tepera-dev-spec.md, FR-G частина 2): чи пропустити паузу цього разу — або тому, що
     * ворота вимкнено на сьогодні (T-4, розділ 2.3 документа), або тому, що людина вже пройшла
     * паузу для цього застосунку менш ніж [PROCEED_DEBOUNCE_MILLIS] тому (буквальна вимога
     * приймання: "повторний тап протягом 30 с після успішного проходу не показує паузу вдруге").
     * Відсутній рядок гейта (видалили ворота, але старий ярлик ще десь спрацював) теж пропускає
     * паузу — не блокувати людину даними, яких більше нема, розділ 2.3 "автономія важливіша".
     */
    suspend fun shouldSkipPause(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val pausedUntil = settingsStore.gatesPausedUntilMillis.first()
        if (pausedUntil > now) return@withContext true
        val gate = dao.getByPackageName(packageName) ?: return@withContext true
        now - gate.lastProceedAtMillis <= PROCEED_DEBOUNCE_MILLIS
    }

    suspend fun getDelaySeconds(packageName: String): Int? = withContext(Dispatchers.IO) {
        dao.getByPackageName(packageName)?.delaySeconds
    }

    suspend fun recordProceed(packageName: String) = withContext(Dispatchers.IO) {
        dao.updateLastProceedAtMillis(packageName, System.currentTimeMillis())
    }

    /** Той самий підхід, що `getLaunchIntentForPackage()`, перевірений спайком T-1 на S23. */
    fun getLaunchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun shortcutIdFor(packageName: String) = "gate_$packageName"

    companion object {
        const val GATE_TARGET_PACKAGE_EXTRA = "gate_target_package"
        private const val PROCEED_DEBOUNCE_MILLIS = 30_000L
    }
}
