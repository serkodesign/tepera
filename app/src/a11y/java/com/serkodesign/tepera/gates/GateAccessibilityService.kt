package com.serkodesign.tepera.gates

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.GateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Тестова збірка (flavor a11y): ворота без ярликів. Подія `TYPE_WINDOW_STATE_CHANGED` дає лише назву пакета вікна, що
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

    override fun onServiceConnected() {
        super.onServiceConnected()
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
        if (pkg == lastPackage) {
            lastSeenMillis[pkg] = now
            return
        }

        val previousSeen = lastSeenMillis[pkg]
        lastPackage = pkg
        lastSeenMillis[pkg] = now
        if (pkg !in gatedPackages) return
        if (previousSeen != null && now - previousSeen <= SAME_SESSION_MILLIS) return

        scope.launch {
            if (gateRepository.shouldSkipPause(pkg)) return@launch
            gateRepository.markPrechecked(pkg)
            ignoreOwnUntilMillis = System.currentTimeMillis() + OWN_WINDOW_MILLIS
            startActivity(
                Intent(this@GateAccessibilityService, MainActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(GateRepository.GATE_TARGET_PACKAGE_EXTRA, pkg)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
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
        "com.google.android.gms"
    )

    private companion object {
        const val SAME_SESSION_MILLIS = 30_000L
        const val OWN_WINDOW_MILLIS = 5_000L
    }
}
