package com.serkodesign.tepera.domain.risk

import java.time.LocalDateTime

/**
 * v3.0 MVP, Етап 8: чи показувати підказку поверх стрічки, яку щойно відкрили. Чиста логіка без Android.
 *
 * Підказка показується всередині ввімкненого вікна на КОЖНЕ нове відкриття стрічки. Дебаунсу нема свідомо: інакше можна
 * було натиснути «Так, роблю це» і одразу відкрити стрічку без жодної перешкоди. Від спаму захищає сервіс доступності:
 * повернення в той самий застосунок за 30 с — та сама сесія, а не нове відкриття.
 */
object PlanOverlayRule {

    data class Candidate(val spec: NextWindow.Spec, val enabled: Boolean)

    /** Ввімкнені вікна, що тривають зараз. Окремо від [pick], щоб сервіс міг записати причину відмови в лог. */
    fun inside(candidates: List<Candidate>, now: LocalDateTime, cfg: RiskConfig = RiskConfig()): List<NextWindow.Occurrence> =
        NextWindow.occurrences(candidates.filter { it.enabled }.map { it.spec }, now, cfg).filter { it.isInside(now) }

    /** Id вікна, чию підказку треба показати зараз, або `null`. */
    fun pick(candidates: List<Candidate>, now: LocalDateTime, cfg: RiskConfig = RiskConfig()): String? =
        inside(candidates, now, cfg).firstOrNull()?.id
}
