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
import com.serkodesign.tepera.util.GateActivity
import com.serkodesign.tepera.util.GatePausePresets
import com.serkodesign.tepera.util.GateSchedule
import com.serkodesign.tepera.util.PauseWindow
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
     * @return true, якщо ворота реально створено; false, якщо `requestPinShortcut()` не
     * підтримується або сам запит відмовив.
     *
     * **Другий реальний баг, знайдений користувачем на Samsung S23 (версія 9, `MIGRATION_8_9`):**
     * попередня версія рахувала ID ярлика наживо, детерміновано з `packageName`
     * (`"gate_" + packageName`) — тож ПОВТОРНЕ створення воріт для застосунку, чиї попередні
     * ворота видаляли (`removeGate()` нижче лише ВИМИКАЄ ярлик — Android не дає застосунку
     * самостійно зняти закріплений ярлик, той ID і далі "існує" в системі), завжди намагалось
     * перевикористати ТОЙ САМИЙ ID. Раніше тут була гілка "якщо ярлик з таким ID уже закріплений,
     * але вимкнений — не пінити заново, а ввімкнути й оновити наявний" (щоб уникнути
     * `IllegalArgumentException`, яку `requestPinShortcut()` кидає для вже існуючого вимкненого
     * ID). Та `enableShortcuts()` НЕ показує системний діалог розміщення на робочому столі —
     * просто знімає прапорець "вимкнено" з запису, який лишається де й був (іноді взагалі не на
     * видимому лаунчері — `dumpsys shortcut` підтвердив: один і той самий детермінований ID міг
     * "прилипнути" до невидимого лаунчера типу `com.google.android.as`/`com.microsoft.launcher`
     * ще з першої спроби). Наслідок: застосунок рапортував "Ярлик створено", а іконка ніде не
     * з'являлась — відтворено буквально (Instagram: видалив ворота, створив заново, жодного
     * системного діалогу, `enableShortcuts()` мовчки "оживив" старий невидимий запис).
     * Фікс — кожне створення генерує СВІЖИЙ унікальний ID ([shortcutId], `AppGateEntity`), якого
     * система ще НІКОЛИ не бачила: `requestPinShortcut()` тоді гарантовано трактує запит як новий
     * і показує справжній діалог розміщення. Стара гілка "увімкнути вимкнений" прибрана
     * повністю — вона більше не потрібна (і не може кинути ту саму виключну ситуацію, яку раніше
     * запобігала, бо ID більше ніколи не повторюється). try/catch лишається як запобіжник від
     * інших системних відмов.
     *
     * **Перший реальний баг (диспетчер виклику):** `requestPinShortcut()` має виконуватись одразу
     * услід за дією користувача (тап "Створити ворота") на диспетчері виклику (Main, з
     * `rememberCoroutineScope()` у GatesScreen) — лаунчер перевіряє, чи застосунок справді щойно
     * на передньому плані, перш ніж показати діалог; перемикання на IO раніше додавало
     * непередбачувану затримку, через яку деякі лаунчери мовчки відмовлялись показати діалог.
     * Лише вставка в Room явно йде на IO — єдина частина, що справді потребує фонового потоку.
     */
    suspend fun createGate(packageName: String, label: String, delaySeconds: Int): Boolean {
        if (!isPinShortcutSupported()) return false

        val id = "gate_${packageName}_${System.currentTimeMillis()}"
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
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } catch (e: Exception) {
            false
        }
        if (requested) {
            withContext(Dispatchers.IO) {
                dao.insert(
                    AppGateEntity(
                        packageName = packageName,
                        delaySeconds = delaySeconds,
                        originalIconHandled = false,
                        shortcutId = id
                    )
                )
            }
        }
        return requested
    }

    /**
     * Android не дає застосунку самостійно зняти вже закріплений ярлик з робочого столу —
     * `disableShortcuts()` (доступний напряму з API 25, minSdk тут 26) лишає його видимим, але
     * тап по ньому більше нічого не відкриває (ShortcutManager сам показує системне пояснення).
     */
    suspend fun removeGate(packageName: String) = withContext(Dispatchers.IO) {
        val shortcutId = dao.getByPackageName(packageName)?.shortcutId
        if (shortcutId != null) {
            context.getSystemService(ShortcutManager::class.java)?.disableShortcuts(listOf(shortcutId))
        }
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
            if (gate.shortcutId !in pinnedIds) {
                dao.deleteByPackageName(gate.packageName)
            }
        }
    }

    /**
     * T-5 (tepera-dev-spec.md, FR-G частина 2) + CC-5: чи пропустити паузу цього разу — або тому, що
     * ворота зараз НЕ активні ([isGateActiveNow]: поза вікном розкладу або на паузі), або тому, що
     * людина вже пройшла паузу для цього застосунку менш ніж [PROCEED_DEBOUNCE_MILLIS] тому (буквальна
     * вимога приймання: "повторний тап протягом 30 с після успішного проходу не показує паузу вдруге").
     * Відсутній рядок гейта (видалили ворота, але старий ярлик ще десь спрацював) теж пропускає
     * паузу — не блокувати людину даними, яких більше нема, розділ 2.3 "автономія важливіша".
     */
    suspend fun shouldSkipPause(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!isGateActiveNow(now)) return@withContext true
        val gate = dao.getByPackageName(packageName) ?: return@withContext true
        now - gate.lastProceedAtMillis <= PROCEED_DEBOUNCE_MILLIS
    }

    suspend fun getDelaySeconds(packageName: String): Int? = withContext(Dispatchers.IO) {
        dao.getByPackageName(packageName)?.delaySeconds
    }
    val pause: Flow<PauseWindow?> = settingsStore.gatePause
    val schedule: Flow<GateSchedule?> = settingsStore.gateSchedule

    /**
     * Ворота активні = зараз вікно розкладу І немає паузи ([GateActivity]). Перед перевіркою
     * закриває вже закінчену паузу (пише `gate_pause_end`).
     */
    suspend fun isGateActiveNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        reconcileExpiredPause(nowMillis)
        val pauses = listOfNotNull(settingsStore.gatePause.first())
        return GateActivity.isActive(nowMillis, settingsStore.gateSchedule.first(), pauses)
    }

    /**
     * Пауза закінчується автоматично: коли ми вперше помічаємо, що вона минула, зберігаємо її в
     * історії, знімаємо й пишемо `gate_pause_end` з часом ЗАКІНЧЕННЯ паузи (не з часом, коли помітили).
     * Ідемпотентно; викликається при кожній перевірці активності, відкритті екрана воріт і скані обходів.
     */
    suspend fun reconcileExpiredPause(nowMillis: Long = System.currentTimeMillis()) {
        val current = settingsStore.gatePause.first() ?: return
        if (nowMillis < current.untilMillis) return
        settingsStore.appendGatePauseHistory(current)
        settingsStore.setGatePause(null)
    }

    /** Ставить паузу ([PauseWindow] з [GatePausePresets]); попередню (якщо була) завершує. */
    suspend fun startPause(window: PauseWindow, nowMillis: Long = System.currentTimeMillis()) {
        endPause(nowMillis)
        settingsStore.setGatePause(window)
    }

    /** "Увімкнути ворота зараз": скасовує паузу (чинну чи заплановану на майбутнє). */
    suspend fun endPause(nowMillis: Long = System.currentTimeMillis()) {
        reconcileExpiredPause(nowMillis)
        val current = settingsStore.gatePause.first() ?: return
        if (current.fromMillis < nowMillis) {
            settingsStore.appendGatePauseHistory(PauseWindow(current.fromMillis, nowMillis))
        }
        settingsStore.setGatePause(null)
    }

    /** `null` = "завжди". Кожне збереження розкладу пишеться як `schedule_changed`. */
    suspend fun setSchedule(schedule: GateSchedule?) {
        settingsStore.setGateSchedule(schedule)
    }

    /** Розклад і паузи (минулі й чинна) для оцінки "чи були ворота активні" в минулому (скан обходів, CC-9). */
    suspend fun activityContext(): Pair<GateSchedule?, List<PauseWindow>> {
        reconcileExpiredPause()
        val pauses = settingsStore.gatePauseHistory.first() + listOfNotNull(settingsStore.gatePause.first())
        return settingsStore.gateSchedule.first() to pauses
    }


    suspend fun setDelaySeconds(packageName: String, delaySeconds: Int) = withContext(Dispatchers.IO) {
        dao.updateDelaySeconds(packageName, delaySeconds)
    }

    suspend fun recordProceed(packageName: String) = withContext(Dispatchers.IO) {
        dao.updateLastProceedAtMillis(packageName, System.currentTimeMillis())
    }

    /** Той самий підхід, що `getLaunchIntentForPackage()`, перевірений спайком T-1 на S23. */
    fun getLaunchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    companion object {
        const val GATE_TARGET_PACKAGE_EXTRA = "gate_target_package"
        private const val PROCEED_DEBOUNCE_MILLIS = 30_000L
    }
}
