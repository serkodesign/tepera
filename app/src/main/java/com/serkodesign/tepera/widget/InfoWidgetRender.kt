package com.serkodesign.tepera.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import com.serkodesign.tepera.ui.home.DayStructure

/**
 * Растрові малюнки інформаційних віджетів. RemoteViews не має Canvas/Brush, а Glance-контейнери обмежені за кількістю
 * дітей (див. коментар до [renderDailyGridBitmap] у TeperaWidget.kt), тож кільце, смуга структури доби й орб малюються
 * одним Bitmap. Розмір — у пікселях точно під місце в макеті, щоб Image не масштабував (без розмиття).
 */

/** Кільце: фон-доріжка на повне коло й дуга [fraction] від 12 години за годинниковою стрілкою (макет — 6dp на 48dp). */
internal fun renderRing(sizePx: Int, fraction: Float, trackArgb: Int, fillArgb: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val stroke = sizePx * 6f / 48f
    val inset = stroke / 2
    val oval = RectF(inset, inset, sizePx - inset, sizePx - inset)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }
    paint.color = trackArgb
    canvas.drawArc(oval, 0f, 360f, false, paint)
    if (fraction > 0f) {
        paint.color = fillArgb
        // Мінімальна дуга — щоб ненульовий Online не зник зовсім.
        canvas.drawArc(oval, -90f, (fraction * 360f).coerceAtLeast(4f), false, paint)
    }
    return bitmap
}

/**
 * Смуга структури доби (той самий принцип, що на Home, [DayStructure]): пропорційні сегменти з проміжком 1dp між
 * ними, ще не прожитий час — кольором доріжки, тиха засічка орієнтиру без підпису (FR-3.10).
 */
internal fun renderStructureBar(
    widthPx: Int,
    heightPx: Int,
    structure: DayStructure,
    trackArgb: Int,
    tickArgb: Int,
    gapPx: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, w, h), h / 2, h / 2, Path.Direction.CW) })
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = trackArgb
    canvas.drawRect(0f, 0f, w, h, paint)
    structure.segments.forEach { segment ->
        // Заштрихований (ще не прожитий) відрізок лишається доріжкою — не малюємо поверх.
        if (segment.hatched) return@forEach
        paint.color = segment.color.toArgb()
        val left = segment.start * w
        val right = (segment.end * w - gapPx).coerceAtLeast(left + 1f)
        canvas.drawRect(left, 0f, right, h, paint)
    }
    structure.targetFraction?.let { fraction ->
        paint.color = tickArgb
        val x = fraction * w
        canvas.drawRect(x - 1f, 0f, x + 1f, h, paint)
    }
    return bitmap
}

/** Орб "без телефону" (як на Home): коло, вода до рівня [level] і хвиля на її поверхні. */
internal fun renderOrb(sizePx: Int, level: Float, bgArgb: Int, waterArgb: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val s = sizePx.toFloat()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = bgArgb
    canvas.drawCircle(s / 2, s / 2, s / 2, paint)
    if (level > 0f) {
        canvas.save()
        canvas.clipPath(Path().apply { addCircle(s / 2, s / 2, s / 2, Path.Direction.CW) })
        paint.color = waterArgb
        val top = s * (1f - level.coerceIn(0f, 1f))
        canvas.drawRect(0f, top, s, s, paint)
        // Хвиля на поверхні: еліпс, ширший за коло, центром на лінії води.
        val waveHalfHeight = s * 0.1f
        canvas.drawOval(RectF(-s * 0.15f, top - waveHalfHeight, s * 1.15f, top + waveHalfHeight), paint)
        canvas.restore()
    }
    return bitmap
}
