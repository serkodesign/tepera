package com.serkodesign.tepera.data.cards

/**
 * T-13 (tepera-dev-spec.md): "джерело картки оголошує: тип, тригер, пріоритет, мінімальний
 * інтервал повтору, вимоги до даних" — сам перелік типів це "оголошення", решта (тригер/
 * пріоритет/інтервал/готовність даних) збирається окремо в [CardSource]. Додавання нового типу
 * картки — новий рядок тут + новий [CardSource] у виклику [CardEngine.selectVisible] — жодних
 * змін усередині рушія (буквальна вимога приймання).
 *
 * [isEstimate] — картки формату "оцінка → реальність" (FR-P.1, T-10, T-14), на які діє глобальний
 * бюджет "не більше 1 картки з оцінкою на тиждень, сумарно по всіх типах". [isEvent] — подієві
 * картки (пауза), на які діє окреме правило "не витісняють тижневі більш ніж двічі поспіль".
 * `ONLINE_ESTIMATE_REVEAL` — одноразове розкриття онбордингу (T-3), навмисно НЕ [isEstimate]:
 * це не періодичний перезапит, а одноразовий показ конкретного вже збереженого результату,
 * тому глобальний тижневий бюджет на нього не поширюється (як і раніше — розкриття не з'являлось
 * власним таймером, лише коли реально є нерозв'язаний запис).
 */
enum class CardType(val isEstimate: Boolean, val isEvent: Boolean) {
    ONLINE_ESTIMATE_REVEAL(isEstimate = false, isEvent = false),
    PAUSE(isEstimate = false, isEvent = true),
    WEEKLY_REFLECTION(isEstimate = true, isEvent = false),
    UNLOCK_ESTIMATE(isEstimate = true, isEvent = false),
    LAST_PHONE_USE_ESTIMATE(isEstimate = true, isEvent = false),
    WEEKLY_DIGEST(isEstimate = false, isEvent = false),
    PATTERN(isEstimate = false, isEvent = false),
    /**
     * T-6 (tepera-dev-spec.md): "свідчення спроможності" (FR-P.3) — тихий факт "цього місяця N
     * разів ти вирішив не зараз" (ворота, T-5). НЕ [isEstimate]: жодного питання/оцінки, лише
     * констатація вже наявного факту — глобальний тижневий бюджет карток-оцінок на неї не діє.
     */
    GATE_EVENTS_SUMMARY(isEstimate = false, isEvent = false)
}
