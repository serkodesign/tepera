package com.serkodesign.tepera.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * W-3: малює смугу дня в Bitmap (Canvas), не `Row` з блоками з вагою. Причина, задокументована за
 * прямою вимогою завдання ("Bitmap допустимий, якщо обґрунтуєш"):
 * 1. "Використання — тонка лінія" вимагає позначок на ДОВІЛЬНІЙ дробовій позиції вздовж смуги —
 *    зваженими блоками `Row` така точність недосяжна без квантування на дрібні слоти.
 * 2. Цей самий проєкт уже мав задокументований баг: RemoteViews-хост (Microsoft Launcher,
 *    Samsung One UI) мовчки відкидає дітей `Row`, коли їх забагато (>~20) — квантування смуги на
 *    дрібні слоти, щоб позначки використання виглядали лінією, відтворило б ту саму проблему.
 *
 * Теми (`GlanceTheme`) тут не втрачається: композиція, що викликає цю функцію, резолвить
 * `ColorProvider` у справжній ARGB ПЕРЕД викликом (через `.getColor(context)`), тож світла й темна
 * тема так само підхоплюються, як і в `Row`-варіанті — просто на крок раніше.
 */
fun renderDayStripBitmap(
    layout: DayStripLayout,
    unnamedGapColor: Int,
    usageTickColor: Int,
    categoryColorForGap: (String) -> Int?,
    widthPx: Int,
    heightPx: Int,
    cornerRadiusPx: Float,
    baseColor: Int,
    muted: Boolean
): Bitmap {
    val w = widthPx.coerceAtLeast(1)
    val h = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
    val clip = Path().apply { addRoundRect(bounds, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW) }

    canvas.save()
    canvas.clipPath(clip)

    paint.color = baseColor
    canvas.drawRect(bounds, paint)

    layout.gapSegments.forEach { segment ->
        paint.color = segment.categoryId?.let(categoryColorForGap) ?: unnamedGapColor
        canvas.drawRect(segment.startFraction * w, 0f, segment.endFraction * w, h.toFloat(), paint)
    }

    // "Тонка лінія" — вузька смужка вздовж нижнього краю, а не заливка на всю висоту (відрізняє
    // Online від заповнених пауз, які займають всю висоту смуги).
    val tickHeight = h * 0.2f
    paint.color = usageTickColor
    layout.usageTicks.forEach { tick ->
        val left = (tick.startFraction * w).coerceAtLeast(0f)
        val right = (tick.endFraction * w).coerceAtLeast(left + 1f) // мінімум 1px — інакше зовсім короткі сесії зникають
        canvas.drawRect(left, h - tickHeight, right, h.toFloat(), paint)
    }

    canvas.restore()

    if (muted) {
        // "Нічне вікно" (FR-3.x): завершена форма дня, приглушено — темний напівпрозорий шар
        // поверх усього, простіше й дешевше, ніж перераховувати кожен колір з іншою альфою.
        paint.color = 0x66000000.toInt()
        canvas.drawPath(clip, paint)
    }

    return bitmap
}
