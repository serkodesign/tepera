package com.serkodesign.tepera.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.GateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Експериментально, НЕ для релізу — PoC-гілка (CLAUDE.md, "AccessibilityService-ворота"):
 * системне перехоплення відкриття застосунку, незалежно від шляху (іконка на робочому столі,
 * recents, сповіщення, асистент) — на відміну від поточного T-4/T-5 механізму (закріплений
 * ярлик з бейджованою іконкою), який перехоплює лише тап по САМОМУ ярлику.
 *
 * Навмисно МІНІМАЛЬНИЙ набір даних: лише `event.packageName` з `TYPE_WINDOW_STATE_CHANGED`
 * (`canRetrieveWindowContent="false"` у конфігу) — сервіс ніколи не читає вміст екрана.
 *
 * **Відомий компроміс (задокументований, не виправлений):** між реальним переходом застосунку
 * на передній план і моментом, коли `onAccessibilityEvent()` встигає викликати `startActivity()`
 * для екрана паузи, є короткий проміжок — цільовий застосунок встигає БУТИ ПОМІТНИМ користувачу
 * до паузи. Поточний shortcut-механізм цього не має (перехоплює ДО запуску). Це одна з причин,
 * чому документ називає цей варіант технічно слабшим, не лише policy-ризикованішим.
 */
class TeperaGateAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    // Дебаунс НЕ пов'язаний з GateRepository.PROCEED_DEBOUNCE_MILLIS (той захищає від повторної
    // паузи ПІСЛЯ успішного проходу) — цей короткий, лише щоб той самий перехід на передній план
    // не спричинив кілька startActivity() підряд через кілька TYPE_WINDOW_STATE_CHANGED-подій
    // одного й того самого переходу (різні внутрішні вікна того самого застосунку).
    private var lastHandledPackage: String? = null
    private var lastHandledAtMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        val now = System.currentTimeMillis()
        if (packageName == lastHandledPackage && now - lastHandledAtMillis < TRIGGER_DEBOUNCE_MILLIS) return

        val app = applicationContext as TeperaApp
        scope.launch {
            val gateRepository = app.gateRepository
            // dao-перевірка через той самий репозиторій, щоб не дублювати доступ до Room напряму
            // з сервісу; shouldSkipPause() уже враховує розклад/паузу/debounce "щойно пройшов".
            if (!gateRepository.hasGate(packageName)) return@launch
            if (gateRepository.shouldSkipPause(packageName)) return@launch

            lastHandledPackage = packageName
            lastHandledAtMillis = now

            val intent = Intent(applicationContext, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(GateRepository.GATE_TARGET_PACKAGE_EXTRA, packageName)
            try {
                applicationContext.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "startActivity failed for $packageName", e)
            }
        }
    }

    override fun onInterrupt() {
        // Системний виклик при вимкненні/конфлікті сервісу — немає стану, що треба скинути негайно.
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private companion object {
        const val TAG = "TeperaGateA11y"
        const val TRIGGER_DEBOUNCE_MILLIS = 1500L
    }
}
