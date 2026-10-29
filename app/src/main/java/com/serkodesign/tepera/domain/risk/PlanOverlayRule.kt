package com.serkodesign.tepera.domain.risk

import java.time.LocalDateTime

/**
 * v3.0 MVP, Етап 8: чи показувати підказку поверх стрічки, яку щойно відкрили. Чиста логіка без Android.
 *
 * Підказка показується лише всередині ввімкненого вікна і не частіше, ніж раз на [DEBOUNCE_MILLIS] для одного вікна:
 * інакше кожне повернення в стрічку за вечір було б «шторкою», а не тихою ідеєю.
 */
object PlanOverlayRule {
    const val DEBOUNCE_MILLIS = 10 * 60_000L

    data class Candidate(val spec: NextWindow.Spec, val enabled: Boolean)

    /** Ввімкнені вікна, що тривають зараз (без урахування дебаунсу). Окремо від [pick], щоб сервіс міг записати причину відмови в лог. */
    fun inside(candidates: List<Candidate>, now: LocalDateTime, cfg: RiskConfig = RiskConfig()): List<NextWindow.Occurrence> =
        NextWindow.occurrences(candidates.filter { it.enabled }.map { it.spec }, now, cfg).filter { it.isInside(now) }

    /** Id вікна, чию підказку треба показати зараз, або `null`. [lastShownMillis] — коли підказку цього вікна показували востаннє (0 — ніколи). */
    fun pick(
        candidates: List<Candidate>,
        now: LocalDateTime,
        nowMillis: Long,
        lastShownMillis: (String) -> Long,
        cfg: RiskConfig = RiskConfig()
    ): String? =
        inside(candidates, now, cfg).firstOrNull { nowMillis - lastShownMillis(it.id) >= DEBOUNCE_MILLIS }?.id
}
