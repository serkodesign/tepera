package com.serkodesign.tepera.gates

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.serkodesign.tepera.R
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
    private var toastView: View? = null

    private val onDeadline = Runnable { showReminder() }
    private val onStrong = Runnable { strengthenDim() }
    private val hideToast = Runnable { removeView(toastView).also { toastView = null } }

    /** Застосунок [packageName] вийшов на передній план: починає сесію, якщо її щойно замовив лист плану, інакше — перевіряє вихід. */
    fun onForeground(packageName: String) {
        val current = session
        if (current == null) {
            val pending = PlanOverlayStore.takePendingSoftExit(service, packageName) ?: return
            start(pending)
        } else if (current.packageName != packageName) {
            finish(onTime = isOnTime(current))
        }
    }

    /** Людина перейшла на робочий стіл. */
    fun onLeftToLauncher() {
        session?.let { finish(onTime = isOnTime(it)) }
    }

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

    private fun isOnTime(s: Session) = System.currentTimeMillis() <= s.deadline + ON_TIME_GRACE_MILLIS

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
            // Макет v25/DimLayer «Ніч»: бурштин 16% під чорним 50%.
            background = android.graphics.drawable.LayerDrawable(arrayOf(ColorDrawable(0x29E69D00), ColorDrawable(0x80000000.toInt())))
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
    }

    private fun showPill(s: Session) {
        val pill = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xFF11322E.toInt(), 28, stroke = 0x1AFFFFFF)
            setPadding(dp(20), dp(18), dp(20), dp(16))
        }
        val head = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        head.addView(label(service.getString(R.string.soft_exit_title, minutesSinceStart(s)), 18f, Color.WHITE, medium = true))
        head.addView(
            label(service.getString(R.string.soft_exit_body, s.appLabel, s.thenText), 12f, 0xBFFFFFFF.toInt()).apply { setPadding(0, dp(2), 0, 0) }
        )
        pill.addView(head)
        val buttons = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, 0) }
        buttons.addView(
            button(R.string.soft_exit_more, outlined = true) { onMore() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) }
        )
        buttons.addView(button(R.string.soft_exit_leave, outlined = false) { leave() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        pill.addView(buttons)

        val params = touchable(width = service.resources.displayMetrics.widthPixels - dp(32)).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(72)
        }
        if (addView(pill, params)) pillView = pill
    }

    private fun onMore() {
        val s = session ?: return
        if (s.extensions >= 1) showConfirm(s) else extend()
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
        val sheet = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF1B1B1D.toInt())
                cornerRadii = floatArrayOf(dp(32).toFloat(), dp(32).toFloat(), dp(32).toFloat(), dp(32).toFloat(), 0f, 0f, 0f, 0f)
            }
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        sheet.addView(label(service.getString(R.string.soft_exit_confirm_title), 27f, Color.WHITE, medium = true))
        sheet.addView(
            label(service.getString(R.string.soft_exit_confirm_body, minutesSinceStart(s)), 14f, 0xFFC5DCD9.toInt()).apply { setPadding(0, dp(6), 0, dp(14)) }
        )
        sheet.addView(
            button(R.string.soft_exit_leave, outlined = false, tall = true, onDark = false) { leave() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        sheet.addView(
            button(R.string.soft_exit_confirm_more, outlined = false, plain = true, tall = true) { extend() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) }
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
            service.getString(if (onTime) R.string.soft_exit_toast_on_time else R.string.soft_exit_toast_late, minutes, SESSION_MINUTES)
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
        val row = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0xFFF9F0DF.toInt(), 100)
            setPadding(dp(12), dp(10), dp(18), dp(10))
        }
        row.addView(
            TextView(service).apply {
                this.text = "✓"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                background = rounded(0xFF006944.toInt(), 14)
            },
            LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(10) }
        )
        row.addView(label(text, 14f, 0xFF1B1B1D.toInt(), medium = true))
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
        removeView(pillView); pillView = null
    }

    // --- Дрібниці вигляду ---

    private fun dp(value: Int) = (value * density).roundToInt()

    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        stroke?.let { setStroke(dp(1), it) }
    }

    private fun label(text: String, sp: Float, color: Int, medium: Boolean = false) = TextView(service).apply {
        this.text = text
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        typeface = if (medium) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT
    }

    private fun button(textRes: Int, outlined: Boolean, plain: Boolean = false, tall: Boolean = false, onDark: Boolean = true, onClick: () -> Unit): View =
        TextView(service).apply {
            text = service.getString(textRes)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            val vertical = if (tall) 15 else 10
            setPadding(dp(if (tall) 16 else 14), dp(vertical), dp(if (tall) 16 else 14), dp(vertical))
            when {
                outlined -> { background = rounded(Color.TRANSPARENT, 100, stroke = 0x59FFFFFF); setTextColor(Color.WHITE) }
                plain -> { setTextColor(0xFFFEFFEF.toInt()) }
                // Світла кнопка: на темній плашці — біла (#fff/#1b1b1d), на листі — кремова (#feffef/#062924).
                else -> {
                    background = rounded(if (onDark) Color.WHITE else 0xFFFEFFEF.toInt(), if (tall) 24 else 100)
                    setTextColor(if (onDark) 0xFF1B1B1D.toInt() else 0xFF062924.toInt())
                }
            }
            setOnClickListener { onClick() }
        }

    private companion object {
        const val SESSION_MINUTES = 5
        const val SESSION_MILLIS = SESSION_MINUTES * 60_000L
        const val ON_TIME_GRACE_MILLIS = 2 * 60_000L
        const val FADE_MILLIS = 3_000L
        const val TOAST_MILLIS = 2_500L
    }
}
