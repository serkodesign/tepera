package com.serkodesign.tepera.domain.risk

import java.time.LocalDateTime

/**
 * v3.0 MVP, Етап 8: чи показувати план поверх стрічки, яку щойно відкрили. Чиста логіка без Android.
 *
 * План показується лише всередині ввімкненого вікна з планом і не частіше, ніж раз на [DEBOUNCE_MILLIS] для одного вікна:
 * інакше кожне повернення в стрічку за вечір було б «шторкою», а не тихою підказкою.
 */
object PlanOverlayRule {
    const val DEBOUNCE_MILLIS = 20 * 60_000L

    data class Candidate(val spec: NextWindow.Spec, val enabled: Boolean, val hasPlan: Boolean)

    /** Id вікна, чий план треба показати зараз, або `null`. [lastShownMillis] — коли план цього вікна показували востаннє (0 — ніколи). */
    fun pick(
        candidates: List<Candidate>,
        now: LocalDateTime,
        nowMillis: Long,
        lastShownMillis: (String) -> Long,
        cfg: RiskConfig = RiskConfig()
    ): String? {
        val usable = candidates.filter { it.enabled && it.hasPlan }
        return NextWindow.occurrences(usable.map { it.spec }, now, cfg)
            .firstOrNull { it.isInside(now) && nowMillis - lastShownMillis(it.id) >= DEBOUNCE_MILLIS }
            ?.id
    }
}
