package com.serkodesign.tepera.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Токени руху Material 3 (m3.material.io/styles/motion/easing-and-duration/tokens-specs): криві
 * "emphasized" і стандартні тривалості. Використовуються для анімацій дизайн-системи (напр. картка
 * категорії на Home: зміна кольору й розтягування кнопки паузи).
 */
object TeperaMotion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val SHORT3 = 150
    const val SHORT4 = 200
    const val MEDIUM2 = 300
    const val LONG2 = 500
}

/** Стандартні специфікації руху M3 для типових випадків (позиція/розмір — emphasized 300 мс; колір/прозорість — швидше). */
object TeperaSpecs {
    fun <T> spatial() = androidx.compose.animation.core.tween<T>(TeperaMotion.MEDIUM2, easing = TeperaMotion.Emphasized)
    fun <T> effects() = androidx.compose.animation.core.tween<T>(TeperaMotion.SHORT4, easing = TeperaMotion.Emphasized)

    // Переходи між екранами (M3 "fade through" / "shared axis"): поява — з затримкою після зникнення
    // попереднього, щоб вони не накладались.
    const val EXIT_FADE_MILLIS = 90
    const val ENTER_FADE_MILLIS = 210
    fun <T> enterFade() = androidx.compose.animation.core.tween<T>(
        ENTER_FADE_MILLIS, delayMillis = EXIT_FADE_MILLIS, easing = androidx.compose.animation.core.LinearOutSlowInEasing
    )
    fun <T> exitFade() = androidx.compose.animation.core.tween<T>(
        EXIT_FADE_MILLIS, easing = androidx.compose.animation.core.LinearEasing
    )
}
