package com.serkodesign.tepera.gates

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.serkodesign.tepera.R
import com.serkodesign.tepera.domain.risk.QuickPuzzles
import com.serkodesign.tepera.domain.risk.SoftExitTiming
import com.serkodesign.tepera.util.PlanOverlayStore
import kotlin.math.roundToInt

/**
 * v3.0 MVP, Етап 8 (beta), макет «04 · М'який вихід»: після «Відкрити на 5 хв» стрічка працює без жодного втручання.
 * Коли 5 хв минуло, екран за 3 с темнішає й теплішає (дотики проходять крізь затемнення), знизу з'являється плашка з
 * планом людини її ж словами й кнопками «Ще 5» / «Вийти». Через +2 хв затемнення стає сильнішим, але далі не росте.
 * Друге «Ще 5» просить підтвердження. Усе малюється вікнами сервісу доступності ([WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY]):
 * додаткового дозволу не потрібно, вміст чужого екрана не читається.
 *
 * Вийшов сам раніше (на робочий стіл чи в інший застосунок) — таймер скасовується й сесія рахується вчасною. «Вчасно» =
 * вихід не пізніше ніж за [ON_TIME_GRACE_MILLIS] після поточного строку (він зсувається на 5 хв після кожного «Ще 5»).
 */
/** Зв'язок між [PlanOverlayActivity] (задачки перед «Ще 5») і [SoftExitController]: обидва в одному процесі. */
object SoftExitPuzzleBridge {
    enum class Result { SOLVED, LEAVE, BACK }

    @Volatile var listener: ((Result) -> Unit)? = null
}

class SoftExitController(private val service: AccessibilityService) {

    private class Session(
        val packageName: String,
        val appLabel: String,
        val thenText: String,
        val startedAt: Long,
        var deadline: Long,
        var extensions: Int = 0,
        var strong: Boolean = false
    )

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    private val density = service.resources.displayMetrics.density

    private var session: Session? = null
    private var dimView: FrameLayout? = null
    private var strongLayer: View? = null
    private var pillView: View? = null
    private var exitPill: SoftExitStyle.ExitPillViews? = null
    private var toastView: View? = null

    /** Контекст служби з мовою, обраною в застосунку (сама служба її не знає): інакше затемнення лишалось англійським. */
    private fun localized() = com.serkodesign.tepera.util.LocaleStore.wrap(service)

    private val onDeadline = Runnable { showReminder() }
    private val onStrong = Runnable { strengthenDim() }
    private val hideToast = Runnable { removeView(toastView).also { toastView = null } }

    /** Застосунок [packageName] вийшов на передній план: починає сесію, якщо її щойно замовив лист плану, інакше — перевіряє вихід. */
    fun onForeground(packageName: String, countsAsLeaving: Boolean) {
        val current = session
        if (current == null) {
            val pending = PlanOverlayStore.takePendingSoftExit(service, packageName) ?: return
            start(pending)
        } else if (current.packageName != packageName && countsAsLeaving && !justStarted(current)) {
            finish(onTime = isOnTime(current))
        }
    }

    /**
     * Лист плану записав сесію «Відкрити на 5 хв»: стартуємо одразу, не чекаючи на подію застосунку (вона не завжди
     * приходить, коли стрічка вже була відкрита під листом — тоді таймер не запускався взагалі). Стару сесію, якщо вона
     * ще висить, замінюємо без підрахунку.
     */
    fun onPendingSessionWritten() {
        val pending = PlanOverlayStore.takeAnyPendingSoftExit(service) ?: return
        if (session != null) {
            session = null
            handler.removeCallbacks(onDeadline)
            handler.removeCallbacks(onStrong)
            removeAll()
        }
        start(pending)
    }

    /** Людина перейшла на робочий стіл. */
    /** Пакет сесії, що стартувала щойно (триває перехід лист → стрічка), інакше null. */
    fun startingPackage(): String? = session?.takeIf { justStarted(it) }?.packageName

    fun onLeftToLauncher() {
        session?.let { if (!justStarted(it)) finish(onTime = isOnTime(it)) }
    }

    /** Перші секунди після старту йде перехід листа плану → стрічка: проміжні вікна виходом не вважаємо. */
    private fun justStarted(s: Session) = System.currentTimeMillis() - s.startedAt < START_GRACE_MILLIS

    fun destroy() {
        session = null
        handler.removeCallbacksAndMessages(null)
        removeAll()
    }

    private fun start(pending: PlanOverlayStore.PendingSoftExit) {
        PlanOverlayStore.recordSoftExitStarted(service)
        session = Session(pending.packageName, pending.appLabel, pending.thenText, pending.startedAtMillis, pending.startedAtMillis + SESSION_MILLIS)
        schedule()
    }

    private fun schedule() {
        val s = session ?: return
        handler.removeCallbacks(onDeadline)
        handler.postDelayed(onDeadline, (s.deadline - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    private fun isOnTime(s: Session) = SoftExitTiming.isOnTime(s.deadline, System.currentTimeMillis())

    private fun minutesSinceStart(s: Session) = ((System.currentTimeMillis() - s.startedAt) / 60_000.0).roundToInt().coerceAtLeast(1)

    // --- Нагадування ---

    private fun showReminder() {
        val s = session ?: return
        s.strong = false
        removeAll()
        showDim()
        showPill(s)
        handler.removeCallbacks(onStrong)
        handler.postDelayed(onStrong, ON_TIME_GRACE_MILLIS)
    }

    private fun showDim() {
        val base = FrameLayout(service).apply {
            // Дизайн-система v3.1 (3.6): чорний 50% + теплий шар signalVivid 18%.
            background = SoftExitStyle(service, dark = true).dimBackground()
            alpha = 0f
        }
        val extra = View(service).apply {
            setBackgroundColor(0x33000000)
            alpha = 0f
        }
        base.addView(extra, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (!addView(base, params)) return
        dimView = base
        strongLayer = extra
        base.animate().alpha(1f).setDuration(FADE_MILLIS).start()
    }

    private fun strengthenDim() {
        val s = session ?: return
        s.strong = true
        strongLayer?.animate()?.alpha(1f)?.setDuration(FADE_MILLIS)?.start()
        // Стадія «+2 хв» (3.6): кнопки плашки розтягуються порівну.
        exitPill?.expand()
    }

    private fun showPill(s: Session) {
        // Дизайн-система v3.1 (3.6 плану): плашка завжди темна (лежить на затемненні), токени — див. SoftExitStyle.
        val text = localized()
        val views = SoftExitStyle(service, dark = true).exitPill(
            title = text.getString(R.string.soft_exit_title, minutesSinceStart(s)),
            body = text.getString(R.string.soft_exit_body, s.appLabel, s.thenText),
            moreText = text.getString(R.string.soft_exit_more),
            leaveText = text.getString(R.string.soft_exit_leave),
            onMore = { onMore() },
            onLeave = { leave() }
        )
        val params = touchable(width = service.resources.displayMetrics.widthPixels - dp(32)).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(72)
        }
        if (addView(views.root, params)) {
            pillView = views.root
            exitPill = views
        }
    }

    private fun onMore() {
        val s = session ?: return
        val press = s.extensions + 1
        val puzzles = QuickPuzzles.puzzlesRequired(press, PlanOverlayStore.arePuzzlesEnabled(service))
        when {
            puzzles > 0 -> askPuzzles(s, puzzles)
            s.extensions >= 1 -> showConfirm(s)
            else -> extend()
        }
    }

    /** З 3-го «Ще 5» за сесію: спершу задачка (та сама механіка, що на екрані «я за тобою слідкую»); «Гаразд, іду» вільний. */
    private fun askPuzzles(s: Session, puzzles: Int) {
        SoftExitPuzzleBridge.listener = { result ->
            SoftExitPuzzleBridge.listener = null
            when (result) {
                SoftExitPuzzleBridge.Result.SOLVED -> extend()
                SoftExitPuzzleBridge.Result.LEAVE -> leave()
                SoftExitPuzzleBridge.Result.BACK -> if (session != null) showReminder()
            }
        }
        removeAll()
        handler.removeCallbacks(onStrong)
        service.startActivity(
            android.content.Intent(service, PlanOverlayActivity::class.java)
                .putExtra(PlanOverlayActivity.EXTRA_SOFT_EXIT_PUZZLES, puzzles)
                .putExtra(PlanOverlayActivity.EXTRA_APP_LABEL, s.appLabel)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun extend() {
        val s = session ?: return
        s.extensions++
        s.deadline = System.currentTimeMillis() + SESSION_MILLIS
        s.strong = false
        PlanOverlayStore.recordSoftExitExtended(service)
        handler.removeCallbacks(onStrong)
        removeAll()
        schedule()
    }

    /** Друге й наступні «Ще 5»: коротке підтвердження без оцінок — «можна, просто вирішуєш свідомо». */
    private fun showConfirm(s: Session) {
        removeView(pillView)
        pillView = null
        exitPill = null
        // Дизайн-система v3.1 (3.5 плану): шторка на токенах теми, див. SoftExitStyle.
        val text = localized()
        val sheet = SoftExitStyle(service).confirmSheet(
            title = text.getString(R.string.soft_exit_confirm_title),
            body = text.getString(R.string.soft_exit_confirm_body, minutesSinceStart(s)),
            leaveText = text.getString(R.string.soft_exit_leave),
            moreText = text.getString(R.string.soft_exit_confirm_more),
            onLeave = { leave() },
            onMore = { extend() }
        )
        val params = touchable(width = WindowManager.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.BOTTOM }
        if (addView(sheet, params)) pillView = sheet
    }

    private fun leave() {
        val s = session ?: return
        val onTime = isOnTime(s)
        val minutes = minutesSinceStart(s)
        finish(onTime = onTime)
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        showToast(
            localized().getString(if (onTime) R.string.soft_exit_toast_on_time else R.string.soft_exit_toast_late, minutes, SESSION_MINUTES)
        )
    }

    private fun finish(onTime: Boolean) {
        if (session == null) return
        if (onTime) PlanOverlayStore.recordSoftExitOnTime(service)
        session = null
        handler.removeCallbacks(onDeadline)
        handler.removeCallbacks(onStrong)
        removeAll()
    }

    // --- Вікна ---

    private fun showToast(text: String) {
        removeView(toastView)
        // Дизайн-система v3.1 (3.7 плану): тост у стилі M3 snackbar, тема — як у застосунку (див. SoftExitStyle).
        val row = SoftExitStyle(service).toast(text)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP; y = dp(470) }
        if (addView(row, params)) toastView = row
        handler.removeCallbacks(hideToast)
        handler.postDelayed(hideToast, TOAST_MILLIS)
    }

    private fun touchable(width: Int) = WindowManager.LayoutParams(
        width, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    private fun addView(view: View, params: WindowManager.LayoutParams): Boolean = try {
        windowManager.addView(view, params)
        true
    } catch (e: RuntimeException) {
        false
    }

    private fun removeView(view: View?) {
        if (view == null) return
        try {
            windowManager.removeView(view)
        } catch (e: RuntimeException) {
            // Вікно вже знято системою.
        }
    }

    private fun removeAll() {
        removeView(dimView); dimView = null; strongLayer = null
        removeView(pillView); pillView = null; exitPill = null
    }

    // --- Дрібниці вигляду ---

    private fun dp(value: Int) = (value * density).roundToInt()

    private companion object {
        const val SESSION_MINUTES = 5
        const val SESSION_MILLIS = SESSION_MINUTES * 60_000L
        const val ON_TIME_GRACE_MILLIS = SoftExitTiming.GRACE_MILLIS
        const val START_GRACE_MILLIS = 4_000L
        const val FADE_MILLIS = 3_000L
        const val TOAST_MILLIS = 2_500L
    }
}
