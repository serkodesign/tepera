package com.serkodesign.tepera.domain.risk

/** Що показує герой головного екрана режиму v3. */
enum class RiskHero {
    /** Година скролінгу триває просто зараз: відлік і одна ідея з кнопкою «Почати». */
    IN_WINDOW,

    /** Поза годиною є непозначені паузи в користуванні телефоном (картка «що ти робив?»). */
    PAUSES,

    /** Поза годиною: відлік до наступної. */
    NEXT_WINDOW,

    /** Поза годиною й без наступної: вчорашній тепловий патерн. */
    PATTERN,

    NONE
}

/**
 * Пріоритет героя (чиста логіка, без Android): година скролінгу, що триває → паузи → наступна година → патерн.
 * Одна головна картка замість стосу: решта лишається нижче як деталі.
 */
object RiskHeroPicker {
    fun pick(insideWindow: Boolean, hasPauses: Boolean, hasNextWindow: Boolean, hasPattern: Boolean): RiskHero = when {
        insideWindow -> RiskHero.IN_WINDOW
        hasPauses -> RiskHero.PAUSES
        hasNextWindow -> RiskHero.NEXT_WINDOW
        hasPattern -> RiskHero.PATTERN
        else -> RiskHero.NONE
    }
}
