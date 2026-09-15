package com.serkodesign.tepera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.cos
import kotlin.math.sin

private const val ICON_SIZE_PX = 108 // базовий розмір adaptive icon
// Adaptive icon "safe zone" — гарантовано видиме коло ~66dp у 108dp полотні (радіус 33px з 54px
// half-canvas), незалежно від форми маски конкретного лаунчера (коло/сквіркл/квадрат). Перша
// версія малювала кільце ПО КРАЮ полотна (радіус ~49px) — на реальному пристрої (Samsung S23,
// One UI) системний діалог "Додати на головний екран" показав ЧИСТУ оригінальну іконку без сліду
// кільця: воно повністю обрізалось маскуванням. SAFE_RADIUS_PX — трохи консервативніше за 33px,
// із запасом.
private const val SAFE_RADIUS_PX = 30f
private const val BADGE_RADIUS_PX = 7f
// За прямим запитом користувача — бейдж зсунуто далі в кут (за межу SAFE_RADIUS_PX), ближче до
// маленького бейджа застосунку-джерела, який сам лаунчер малює в тому ж куті (One UI) — цей
// системний елемент лишається видимим на тій самій відстані на живому пристрої (Samsung S23,
// скляні/сквіркл-маски), тож невеликий запас за строгу safe zone тут прийнятний; повний
// консервативний радіус лишається для дуже маленьких/повністю округлих (коло) масок.
private const val BADGE_CENTER_DISTANCE_PX = 34f
private val BADGE_COLOR = Color.parseColor("#005E3E") // TeperaPalette.brandAccent

/**
 * T-4 (tepera-dev-spec.md): іконка закріпленого ярлика воріт — НЕ точна копія оригінальної
 * іконки застосунку (ризик відхилення Google Play за політикою про введення в оману). Оригінал
 * лишається впізнаваним (людина мусить розуміти, який застосунок відкриє ярлик), з доданим
 * маленьким куточком-бейджем бренд-кольору Tepera — документ прямо називає куточок прийнятною
 * альтернативою поруч із кільцем; куточок обрано, бо лишається видимим у межах "safe zone"
 * adaptive-іконки на всіх лаунчерах.
 * **За прямим запитом користувача — бейдж зменшено й білу обвідку прибрано** (менше "зайвої"
 * графіки, іконка читається ближче до оригінальної, лишається лише тихий натяк-крапка).
 */
fun buildGateShortcutIcon(context: Context, packageName: String): IconCompat {
    val original = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()

    val bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = ICON_SIZE_PX / 2f

    if (original != null) {
        val inset = 6
        original.setBounds(inset, inset, ICON_SIZE_PX - inset, ICON_SIZE_PX - inset)
        original.draw(canvas)
    }

    // Центр бейджа — на відстані BADGE_CENTER_DISTANCE_PX від центру полотна під 45°.
    val angle = Math.toRadians(45.0)
    val badgeCenterX = center + (BADGE_CENTER_DISTANCE_PX * cos(angle)).toFloat()
    val badgeCenterY = center + (BADGE_CENTER_DISTANCE_PX * sin(angle)).toFloat()

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BADGE_COLOR }
    canvas.drawCircle(badgeCenterX, badgeCenterY, BADGE_RADIUS_PX, badgePaint)

    return IconCompat.createWithBitmap(bitmap)
}
