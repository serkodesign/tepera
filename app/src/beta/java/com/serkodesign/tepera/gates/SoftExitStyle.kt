package com.serkodesign.tepera.gates

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.DarkTeperaExtraColors
import com.serkodesign.tepera.ui.theme.LightTeperaExtraColors
import com.serkodesign.tepera.ui.theme.TeperaDarkColors
import com.serkodesign.tepera.ui.theme.TeperaLightColors
import com.serkodesign.tepera.util.ThemeStore
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Дизайн-система v3.1 для оверлеїв сервісу доступності ([SoftExitController]). Оверлей малюється звичайними `View`
 * у вікні [android.view.WindowManager] (не Compose), тому тут ті самі значення, що в компонентах Compose, але
 * побудовані як `View`: кольори — з `TeperaLightColors`/`TeperaDarkColors` (джерело — `M3Palette`), типографіка —
 * шкала M3 на Golos Text, форми й відступи — за розділами 2.3–2.5 плану `CLAUDE-CODE-PLAN-m3-v31.md`.
 *
 * Тема: [isDark] — як у `TeperaTheme`: вибір у Налаштуваннях (Світла/Темна) або системна.
 */
internal class SoftExitStyle(private val context: Context, val dark: Boolean = isDark(context)) {

    enum class ButtonType { FILLED, TONAL, OUTLINED, TEXT }
    enum class ButtonSize { S, M }

    private val density = context.resources.displayMetrics.density
    private val scheme = if (dark) TeperaDarkColors else TeperaLightColors
    private val golos: Typeface = ResourcesCompat.getFont(context, R.font.golos_text) ?: Typeface.DEFAULT

    fun dp(value: Int) = (value * density).roundToInt()

    private val primary get() = scheme.primary.toArgb()
    private val onPrimary get() = scheme.onPrimary.toArgb()
    private val onSurface get() = scheme.onSurface.toArgb()
    private val onSurfaceVariant get() = scheme.onSurfaceVariant.toArgb()
    private val surfaceContainerLow get() = scheme.surfaceContainerLow.toArgb()
    private val surfaceContainerHighest get() = scheme.surfaceContainerHighest.toArgb()
    private val outlineVariant get() = scheme.outlineVariant.toArgb()
    private val surfaceContainerHigh get() = scheme.surfaceContainerHigh.toArgb()
    private val extra get() = if (dark) DarkTeperaExtraColors else LightTeperaExtraColors
    private val signal get() = extra.signal.toArgb()
    private val signalVivid get() = extra.signalVivid.toArgb()
    private val inverseSurface get() = scheme.inverseSurface.toArgb()
    private val inverseOnSurface get() = scheme.inverseOnSurface.toArgb()
    private val inverseSignal get() = extra.inverseSignal.toArgb()

    /** Текст за стилем M3: розмір/інтервал рядка/інтервал літер у sp, вага — Golos Text (variable, вісь `wght`). */
    fun text(value: String, sizeSp: Float, lineSp: Float, weight: Int, spacingSp: Float, color: Int) = TextView(context).apply {
        text = value
        typeface = golos
        paint.fontVariationSettings = "'wght' $weight"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        letterSpacing = spacingSp / sizeSp
        val lineHeightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, lineSp, resources.displayMetrics)
        val fm = paint.fontMetrics
        setLineSpacing(max(0f, lineHeightPx - (fm.descent - fm.ascent)), 1f)
    }

    /** headlineSmall: 24/32, Medium, −0.12 — заголовок шторок і діалогів. */
    fun headline(value: String) = text(value, 24f, 32f, 500, -0.12f, onSurface)

    /** titleMedium: 16/24, Medium — заголовок плашки виходу. */
    fun title(value: String) = text(value, 16f, 24f, 500, 0f, onSurface)

    /** bodyMedium: 14/20, Normal — основний текст. */
    fun body(value: String, color: Int = onSurfaceVariant) = text(value, 14f, 20f, 400, 0f, color)

    /** Кнопка за розділом 3.1 плану: S — 40dp, M — 56dp, форма full; текст S — labelLarge 14/20, M — titleMedium 16/24. */
    fun button(type: ButtonType, size: ButtonSize, label: String, onClick: () -> Unit): TextView {
        val isM = size == ButtonSize.M
        // Text-кнопка без фону: висота 48dp (мінімальна ціль дотику M3) невидима, але дає зручне натискання.
        val height = dp(if (isM) 56 else if (type == ButtonType.TEXT) 48 else 40)
        val horizontal = dp(if (isM) 24 else if (type == ButtonType.TEXT) 12 else 16)
        val (container, content) = when (type) {
            ButtonType.FILLED -> primary to onPrimary
            ButtonType.TONAL -> surfaceContainerHighest to onSurface
            ButtonType.OUTLINED -> Color.TRANSPARENT to primary
            ButtonType.TEXT -> Color.TRANSPARENT to primary
        }
        return text(label, if (isM) 16f else 14f, if (isM) 24f else 20f, 500, 0f, content).apply {
            gravity = Gravity.CENTER
            minHeight = height
            setPadding(horizontal, 0, horizontal, 0)
            background = GradientDrawable().apply {
                setColor(container)
                cornerRadius = height / 2f
                if (type == ButtonType.OUTLINED) setStroke(dp(1), outlineVariant)
            }
            isClickable = true
            setOnClickListener { onClick() }
        }
    }

    /** Контейнер шторки (3.4): `surfaceContainerLow`, 28dp лише зверху. */
    private fun sheetBackground() = GradientDrawable().apply {
        setColor(surfaceContainerLow)
        val r = dp(28).toFloat()
        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
    }

    /** Ручка шторки: 32×4dp, `onSurfaceVariant` з альфою 0.4. */
    private fun handle() = View(context).apply {
        background = GradientDrawable().apply {
            setColor((onSurfaceVariant and 0x00FFFFFF) or (0x66 shl 24))
            cornerRadius = dp(2).toFloat()
        }
    }

    /**
     * `MoreConfirmSheet` (3.5): оболонка шторки, заголовок, текст (8dp під заголовком), `Filled M` «Вийти» на всю
     * ширину й під нею `Text S` на всю ширину. Поля 24dp з боків і знизу, 8dp зверху після ручки, проміжки 16dp.
     */
    fun confirmSheet(title: String, body: String, leaveText: String, moreText: String, onLeave: () -> Unit, onMore: () -> Unit): View {
        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = sheetBackground()
        }
        sheet.addView(
            handle(),
            LinearLayout.LayoutParams(dp(32), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(14) }
        )
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(24))
        }
        content.addView(headline(title))
        content.addView(
            body(body),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        )
        content.addView(
            button(ButtonType.FILLED, ButtonSize.M, leaveText, onLeave),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
        )
        content.addView(
            button(ButtonType.TEXT, ButtonSize.S, moreText, onMore),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        )
        sheet.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return sheet
    }

    /**
     * Затемнення під плашкою: нейтральний чорний 50% (теплого відтінку немає — за рішенням власника). Дотики крізь нього проходять
     * (прапорці вікна задає контролер).
     */
    fun dimBackground(): Drawable = ColorDrawable(0x80000000.toInt())

    /**
     * `ExitToast` (3.7, стиль M3 snackbar): фон `inverseSurface`, форма 4dp, поля 12×16dp, висота від 48dp, проміжок 12dp;
     * галочка 20dp кольору `inverseSignal`, текст bodyMedium кольору `inverseOnSurface`.
     */
    fun toast(value: String): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        background = GradientDrawable().apply { setColor(inverseSurface); cornerRadius = dp(4).toFloat() }
        setPadding(dp(16), dp(12), dp(16), dp(12))
        addView(
            ImageView(context).apply {
                setImageResource(R.drawable.ic_soft_exit_check)
                setColorFilter(inverseSignal)
            },
            LinearLayout.LayoutParams(dp(20), dp(20)).apply { rightMargin = dp(12) }
        )
        addView(body(value, color = inverseOnSurface))
    }

    /** Плашка виходу й її кнопки: у стадії «+2 хв» кнопки розтягуються порівну ([expand]). */
    class ExitPillViews(val root: View, private val more: View, private val leave: View) {
        fun expand() {
            for (button in listOf(more, leave)) {
                button.layoutParams = (button.layoutParams as LinearLayout.LayoutParams).apply { width = 0; weight = 1f }
            }
            root.requestLayout()
        }
    }

    /**
     * `ExitPill` (3.6): `surfaceContainerHigh`, форма 28dp, без обвідки, поля 16dp зверху/знизу й 20dp з боків, проміжки
     * 12dp. Голова — іконка 22dp `signal`, заголовок titleMedium, підпис bodyMedium. Кнопки в рядку з проміжком 8dp:
     * `Outlined S` «Ще 5» і `Filled S` «Вийти» — за вмістом у компактній стадії, `weight(1f)` після [ExitPillViews.expand].
     * Плашка завжди темна: створюйте [SoftExitStyle] з `dark = true`.
     */
    fun exitPill(title: String, body: String, moreText: String, leaveText: String, onMore: () -> Unit, onLeave: () -> Unit): ExitPillViews {
        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(surfaceContainerHigh); cornerRadius = dp(28).toFloat() }
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        val head = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(
            ImageView(context).apply {
                setImageResource(R.drawable.ic_soft_exit_clock)
                setColorFilter(signal)
            },
            LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(12) }
        )
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(title(title))
        texts.addView(body(body))
        head.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        pill.addView(head)
        val more = button(ButtonType.OUTLINED, ButtonSize.S, moreText, onMore)
        val leave = button(ButtonType.FILLED, ButtonSize.S, leaveText, onLeave)
        val buttons = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        buttons.addView(more, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) })
        buttons.addView(leave, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        // MATCH_PARENT: інакше після expand() вага кнопок не має вільного місця (кнопки лишаються за вмістом і текст переноситься).
        pill.addView(buttons, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        return ExitPillViews(pill, more, leave)
    }

    companion object {
        /** Як `TeperaTheme`: примусова Світла/Темна з Налаштувань, інакше — тема системи. */
        fun isDark(context: Context): Boolean {
            ThemeStore.load(context)
            return when (ThemeStore.current()) {
                ThemeStore.Mode.DARK -> true
                ThemeStore.Mode.LIGHT -> false
                else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
