package com.serkodesign.tepera.gates

import android.accessibilityservice.AccessibilityService
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
import com.serkodesign.tepera.util.PlanOverlayStore
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private var ignoredPackages: Set<String> = emptySet()

    private var lastPackage: String? = null
    private var ignoreOwnUntilMillis = 0L
    private val lastSeenMillis = HashMap<String, Long>()
    private lateinit var softExit: SoftExitController
    private var pendingListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        softExit = SoftExitController(this)
        pendingListener = PlanOverlayStore.listenForPendingSoftExit(this) { softExit.onPendingSessionWritten() }
        gateRepository = (application as TeperaApp).gateRepository
        launcherPackages = resolveLauncherPackages()
        ignoredPackages = buildIgnoredPackages()
        scope.launch {
            gateRepository.gates.collectLatest { gates -> gatedPackages = gates.map { it.packageName }.toSet() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        if (pkg in launcherPackages) {
            // Робочий стіл: сесія закінчилась.
            softExit.onLeftToLauncher()
            lastPackage = pkg
            lastSeenMillis.clear()
            return
        }
        if (pkg == packageName) {
            // Власне вікно паузи (щойно запущене сервісом) застосунком не рахуємо; будь-яке інше відкриття Tepera — це вихід з попереднього.
            if (now < ignoreOwnUntilMillis) return
            lastPackage = pkg
            return
        }
        if (pkg in ignoredPackages || pkg in inputMethodPackages()) return
        // Лише справжні застосунки (з іконкою запуску): службові вікна виробника виходом зі стрічки не вважаємо.
        if (BuildConfig.V3_MVP) softExit.onForeground(pkg, countsAsLeaving = packageManager.getLaunchIntentForPackage(pkg) != null)
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
            // Ворота (затримка) важливіші за план: якщо пауза воріт показується, план цього разу не потрібен.
            if (pkg in gatedPackages && !gateRepository.shouldSkipPause(pkg)) {
                gateRepository.markPrechecked(pkg)
                launchTepera(pkg, planWindowId = null)
                return@launch
            }
            if (BuildConfig.V3_MVP) showPlanIfInsideWindow(pkg)
        }
    }

    /** Етап 8: стрічка відкрита всередині ввімкненого вікна — показуємо підказку, що зробити замість, поверх неї. */
    private suspend fun showPlanIfInsideWindow(pkg: String) {
        // Вимкнути план можна лише зсередини Tepera (перемикач у налаштуваннях, пауза чи вимкнення вікна) — з самого листа чи сповіщення ні.
        if (!PlanOverlayStore.isEnabled(this)) return log("$pkg: план поверх стрічки вимкнено в налаштуваннях")
        val app = application as TeperaApp
        if (pkg !in app.feedAppRepository.packages()) return log("$pkg: не в списку стрічок")
        val candidates = app.riskWindowRepository.getAll().map { w ->
            PlanOverlayRule.Candidate(
                NextWindow.Spec(w.id, w.startMinute, w.endMinute, w.dayType),
                enabled = !w.isUserDisabled && !w.isPausedAt(System.currentTimeMillis())
            )
        }
        val nowMillis = System.currentTimeMillis()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val windowId = PlanOverlayRule.pick(candidates, now, nowMillis, { PlanOverlayStore.lastShownMillis(this, it) })
        if (windowId == null) {
            val inside = PlanOverlayRule.inside(candidates, now)
            if (inside.isEmpty()) {
                log("$pkg: зараз нема активного вікна (вікон: ${candidates.size}, ввімкнених і не на паузі: ${candidates.count { it.enabled }})")
            } else {
                val left = inside.minOf { PlanOverlayRule.DEBOUNCE_MILLIS - (nowMillis - PlanOverlayStore.lastShownMillis(this, it.id)) } / 1000
                log("$pkg: вікно активне, але підказку вже показували — дебаунс ще $left с")
            }
            return
        }
        log("$pkg: показую підказку вікна $windowId")
        PlanOverlayStore.recordShown(this, windowId, nowMillis)
        launchTepera(pkg, planWindowId = windowId)
    }

    /** Причина показу чи відмови оверлея — лише в debug-збірці (назви застосунків у release-логи не пишемо). */
    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, message)
    }

    private fun launchTepera(pkg: String, planWindowId: String?) {
        ignoreOwnUntilMillis = System.currentTimeMillis() + OWN_WINDOW_MILLIS
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

    private fun buildIgnoredPackages(): Set<String> = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.intentresolver",
        "com.google.android.gms",
        // Samsung: панель жестів/навігації — службове вікно, не застосунок.
        "com.samsung.android.sidegesturepad"
    )

    private companion object {
        const val LOG_TAG = "TeperaPlan"
        const val SAME_SESSION_MILLIS = 30_000L
        const val OWN_WINDOW_MILLIS = 5_000L
    }
}
