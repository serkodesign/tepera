package com.serkodesign.tepera.gates

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.isPausedAt
import com.serkodesign.tepera.data.repository.GateRepository
import com.serkodesign.tepera.domain.risk.NextWindow
import com.serkodesign.tepera.domain.risk.PlanOverlayRule
import com.serkodesign.tepera.util.A11yConsentStore
import com.serkodesign.tepera.util.PackageAction
import com.serkodesign.tepera.util.ScreenPackageRule
import com.serkodesign.tepera.util.SoftExitSessionBridge
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.queryEventsOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Тестова збірка (flavor beta): ворота без ярликів. Подія `TYPE_WINDOW_STATE_CHANGED` дає лише назву пакета вікна, що
 * з'явилось (вміст екрана не читається — `canRetrieveWindowContent = false`).
 *
 * **Паузу показуємо лише на ПЕРЕХОДІ в застосунок, а не поки він уже відкритий** (раніше з цим були проблеми):
 * 1. [lastPackage] — останній «справжній» застосунок на екрані; подія для того самого пакета (інша activity, діалог,
 *    прокрутка) нічого не запускає.
 * 2. Службові вікна не рахуються застосунком і не скидають стан: клавіатура, SystemUI (шторка, гучність, недавні),
 *    системні діалоги дозволів/вибору, сама Tepera. Інакше «Instagram → клавіатура → Instagram» виглядало б як нове
 *    відкриття.
 * 3. [SAME_SESSION_MILLIS]: повернення в той самий застосунок протягом 30 с після того, як його востаннє бачили, —
 *    та сама сесія (коротка відлучка в системний діалог чи сповіщення), не нове відкриття.
 * 4. Вихід на робочий стіл (лаунчер) скидає сесію, тож наступне відкриття — нове.
 * 5. Після «Продовжити» діє 30-секундний дебаунс [GateRepository.shouldSkipPause].
 */
class GateAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var gateRepository: GateRepository

    @Volatile private var gatedPackages: Set<String> = emptySet()
    private var launcherPackages: Set<String> = emptySet()

    // Стан сесій живе в companion (на рівні процесу): Samsung іноді перестворює екземпляр служби між кроками, і тоді
    // «останній застосунок» скидався в null — відкриття стрічки з листа плану вважалось новим і давало ще один лист.
    private var lastPackage: String?
        get() = sLastPackage
        set(v) { sLastPackage = v }
    private var lastLaunchedPackage: String?
        get() = sLastLaunchedPackage
        set(v) { sLastLaunchedPackage = v }
    private var lastLaunchMillis: Long
        get() = sLastLaunchMillis
        set(v) { sLastLaunchMillis = v }
    private val lastSeenMillis: HashMap<String, Long> get() = sLastSeenMillis
    private lateinit var softExit: SoftExitController
    private var pendingListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var consentListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    /** Згода на розкриття (кеш; оновлюється слухачем). Без неї служба нічого не читає, не показує і не запускає. */
    @Volatile private var consentAccepted = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        consentAccepted = A11yConsentStore.isAccepted(this)
        consentListener = A11yConsentStore.listen(this) { consentAccepted = A11yConsentStore.isAccepted(this) }
        softExit = SoftExitController(this)
        pendingListener = PlanOverlayStore.listenForPendingSoftExit(this) {
            if (!consentAccepted) return@listenForPendingSoftExit
            softExit.onPendingSessionWritten()
            softExit.startingPackage()?.let {
                sOpenFivePackage = it
                sOpenFiveAtMillis = System.currentTimeMillis()
                lastPackage = it
                lastSeenMillis[it] = sOpenFiveAtMillis
            }
        }
        gateRepository = (application as TeperaApp).gateRepository
        launcherPackages = resolveLauncherPackages()
        SoftExitSessionBridge.endSession = { softExit.endNow() }
        softExit.foregroundCheck = { realForegroundPackage() }
        scope.launch {
            gateRepository.gates.collectLatest { gates -> gatedPackages = gates.map { it.packageName }.toSet() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Без згоди на розкриття служба одразу виходить: пакет не читається, нічого не показується й не запускається.
        if (!consentAccepted) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()
        log("подія: $pkg (lastPackage=$lastPackage)")

        if (pkg in launcherPackages) {
            // Робочий стіл: сесія закінчилась. Але Samsung шле «подію лаунчера» і без виходу на стіл (закрили шторку
            // сповіщень — застосунок під нею лишився), тому спершу звіряємось із системною історією переднього плану.
            scope.launch {
                delay(LAUNCHER_VERIFY_DELAY_MILLIS)
                val foreground = realForegroundPackage()
                if (foreground != null && foreground != pkg && foreground in lastSeenMillis) {
                    // Samsung шле «подію лаунчера» при закритті шторки (після події systemui на відкритті), а інколи й при
                    // відкритті без systemui: тому перемикаємо — сховано → повертаємо, видно → ховаємо.
                    if (softExit.isHiddenForSystemSurface()) softExit.restoreFromSystemSurface()
                    else if (!softExit.restoredRecently()) softExit.hideForSystemSurface(shadeLike = false) // жест «назад»: лише на мить
                    log("$pkg: подія лаунчера фантомна (на екрані досі $foreground) — сесію не скидаю")
                    return@launch
                }
                softExit.onLeftToLauncher()
                lastPackage = pkg
                lastSeenMillis.clear()
            }
            return
        }
        if (pkg == packageName) {
            // Вікна самої Tepera (пауза, лист плану, відкриття зі сповіщення) виходом зі стрічки не вважаємо: інакше «зайшов у
            // стрічку → торкнувся сповіщення Tepera → повернувся» чи довге читання листа давали НОВЕ відкриття і другий лист.
            // Сесію закінчує лише робочий стіл (вихід з листа/паузи веде туди) або інший справжній застосунок.
            return
        }
        when (ScreenPackageRule.classify(pkg, packageName, inputMethodPackages = inputMethodPackages())) {
            PackageAction.END_SESSION -> {
                // Налаштування, Google Play, інсталятор, діалоги дозволів: затемнення й плашка зникають одразу, нових листів нема.
                softExit.endNow()
                return
            }
            PackageAction.IGNORE -> {
                if (pkg == "com.android.systemui") {
                    val cls = event.className?.toString().orEmpty()
                    log("systemui: className=$cls")
                    // className у systemui завжди FrameLayout (перевірено на S23) — відрізнити шторку нема за чим.
                    softExit.hideForSystemSurface(shadeLike = true)
                }
                return
            }
            PackageAction.NORMAL -> Unit
        }
        // Лише справжні застосунки (з іконкою запуску): службові вікна виробника (панелі, віджети, спливаючі вікна) не
        // скидають сесію стрічки — раніше саме вони давали «оверлей просто так».
        val isRealApp = packageManager.getLaunchIntentForPackage(pkg) != null
        if (!isRealApp) return
        softExit.restoreFromSystemSurface()
        if (BuildConfig.V3_MVP) softExit.onForeground(pkg, countsAsLeaving = true)
        if (pkg == lastPackage) {
            lastSeenMillis[pkg] = now
            log("$pkg: той самий застосунок уже на екрані — перехід не новий")
            return
        }

        val previousSeen = lastSeenMillis[pkg]
        lastPackage = pkg
        lastSeenMillis[pkg] = now
        if (previousSeen != null && now - previousSeen <= SAME_SESSION_MILLIS) {
            log("$pkg: повернення за ${(now - previousSeen) / 1000} с — та сама сесія (до ${SAME_SESSION_MILLIS / 1000} с)")
            return
        }

        scope.launch {
            // Не показуємо одразу два листи поспіль для того самого застосунку (кілька подій відкриття майже одночасно).
            if (pkg == lastLaunchedPackage && now - lastLaunchMillis < LAUNCH_DEDUPE_MILLIS) return@launch
            // Ворота (затримка) важливіші за план: якщо пауза воріт показується, план цього разу не потрібен.
            if (pkg in gatedPackages && !gateRepository.shouldSkipPause(pkg)) {
                gateRepository.markPrechecked(pkg)
                launchTepera(pkg, planWindowId = null)
                return@launch
            }
            if (BuildConfig.V3_MVP) showPlanIfInsideWindow(pkg)
        }
    }

    /**
     * Застосунок, що за UsageStats зараз на передньому плані: останній MOVE_TO_FOREGROUND без пізнішого
     * MOVE_TO_BACKGROUND того самого пакета. `null` — не вдалося визначити (нема доступу/збій) або застосунок пішов у фон.
     */
    private fun realForegroundPackage(): String? = try {
        val usm = getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val events = usm?.queryEventsOrNull(end - FOREGROUND_LOOKBACK_MILLIS, end)
        var current: String? = null
        if (events != null) {
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                when (e.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> current = e.packageName
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> if (e.packageName == current) current = null
                }
            }
        }
        current
    } catch (_: SecurityException) {
        null
    }

    /** Етап 8: стрічка відкрита всередині ввімкненого вікна — показуємо підказку, що зробити замість, поверх неї. */
    private suspend fun showPlanIfInsideWindow(pkg: String) {
        // Вимкнути план можна лише зсередини Tepera (перемикач у налаштуваннях, пауза чи вимкнення вікна) — з самого листа чи сповіщення ні.
        if (!PlanOverlayStore.isEnabled(this)) return log("$pkg: план поверх стрічки вимкнено в налаштуваннях")
        val app = application as TeperaApp
        // Тиждень спостереження — база для порівняння: поки період не заданий (перший перерахунок ще не відбувся), Tepera лише дивиться,
        // навіть якщо людина сама додала вікно.
        if (app.database.observationPeriodDao().getLatest() == null) return log("$pkg: тиждень спостереження ще триває — мовчу")
        if (pkg !in app.feedAppRepository.packages()) return log("$pkg: не в списку стрічок")
        if (pkg == sOpenFivePackage && System.currentTimeMillis() - sOpenFiveAtMillis < OPEN_FIVE_QUIET_MILLIS) {
            return log("$pkg: людина щойно обрала «Відкрити на 5 хв» — нового листа не показую")
        }
        val candidates = app.riskWindowRepository.getAll().map { w ->
            PlanOverlayRule.Candidate(
                NextWindow.Spec(w.id, w.startMinute, w.endMinute, w.dayType),
                // «Не сьогодні» з Головної: вікно мовчить до кінця свого найближчого проходу.
                enabled = !w.isUserDisabled && !w.isPausedAt(System.currentTimeMillis()) &&
                    !com.serkodesign.tepera.util.WindowSkipStore.isSkipped(this, w.id)
            )
        }
        val nowMillis = System.currentTimeMillis()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val windowId = PlanOverlayRule.pick(candidates, now)
        if (windowId == null) {
            log("$pkg: зараз нема активного вікна (вікон: ${candidates.size}, ввімкнених і не на паузі: ${candidates.count { it.enabled }})")
            return
        }
        log("$pkg: показую підказку вікна $windowId")
        PlanOverlayStore.recordShown(this)
        launchTepera(pkg, planWindowId = windowId)
    }

    /** Причина показу чи відмови оверлея — лише в debug-збірці (назви застосунків у release-логи не пишемо). */
    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.i(LOG_TAG, message)
    }

    private fun launchTepera(pkg: String, planWindowId: String?) {
        // Поки читали БД, людина могла вже піти зі стрічки — тоді запізнілий лист був би «просто так».
        if (lastPackage != pkg) return log("$pkg: людина вже пішла зі стрічки — лист не потрібен")
        lastLaunchedPackage = pkg
        lastLaunchMillis = System.currentTimeMillis()
        // Пауза воріт — екран Tepera; лист плану — прозора activity поверх стрічки (стрічка лишається видимою під затемненням).
        val intent = if (planWindowId == null) {
            Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        } else {
            Intent(this, PlanOverlayActivity::class.java)
                .putExtra(GateRepository.PLAN_WINDOW_EXTRA, planWindowId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent.putExtra(GateRepository.GATE_TARGET_PACKAGE_EXTRA, pkg))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        pendingListener?.let { PlanOverlayStore.stopListening(this, it) }
        consentListener?.let { A11yConsentStore.stopListening(this, it) }
        SoftExitSessionBridge.endSession = null
        if (::softExit.isInitialized) softExit.destroy()
        scope.cancel()
        super.onDestroy()
    }

    private fun resolveLauncherPackages(): Set<String> =
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
        ).map { it.activityInfo.packageName }.toSet()

    private fun inputMethodPackages(): Set<String> =
        getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.map { it.packageName }?.toSet().orEmpty()

    private companion object {
        @Volatile var sLastPackage: String? = null
        @Volatile var sLastLaunchedPackage: String? = null
        @Volatile var sLastLaunchMillis = 0L
        val sLastSeenMillis = HashMap<String, Long>()
        /** Остання вибрана «Відкрити на 5 хв»: поки триває сесія, нового листа для цього застосунку не показуємо. */
        @Volatile var sOpenFivePackage: String? = null
        @Volatile var sOpenFiveAtMillis = 0L
        const val OPEN_FIVE_QUIET_MILLIS = 10 * 60_000L
        const val LOG_TAG = "TeperaPlan"
        const val SAME_SESSION_MILLIS = 30_000L
        const val LAUNCH_DEDUPE_MILLIS = 3_000L
        const val LAUNCHER_VERIFY_DELAY_MILLIS = 700L
        const val FOREGROUND_LOOKBACK_MILLIS = 30 * 60_000L
    }
}
