package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * T-6 (tepera-dev-spec.md, "події воріт і свідчення спроможності"): один запис на КОЖНЕ реальне
 * розв'язання екрана паузи воріт (T-5) — записується лише коли пауза справді була показана
 * людині (`GatePauseViewModel`); шлях "пропустити паузу" (дебаунс 30с, "вимкнено на сьогодні")
 * НЕ створює запису — там не було моменту вибору, нема що фіксувати.
 *
 * Годує картку FR-P.3 ("свідчення компетентності замість натхнення") буквальним фактом "цього
 * місяця N разів ти вирішив не зараз" — рахує лише [GateEventResult.CANCELLED]. Документ прямо
 * забороняє показувати [GateEventResult.PROCEEDED] як невдачу чи рахувати співвідношення
 * пройдено/скасовано — тому ця сутність зберігає ОБИДВА результати (повна історія), але
 * `GateEventRepository`/`GateEventsSummaryViewModel` читають з неї лише кількість CANCELLED.
 */
@Entity(tableName = "gate_events")
data class GateEventEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val result: String,
    val atMillis: Long
)

enum class GateEventResult { PROCEEDED, CANCELLED }
