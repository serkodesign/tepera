package com.serkodesign.tepera.widget

import com.serkodesign.tepera.util.localStartOfDay
import java.util.Calendar

/**
 * W-1 (`CLAUDE-CODE-TASK-widgets.md`): DST-безпечні зсуви дат для [DaySnapshot]/[WeekSnapshot].
 * `Calendar.add()` нормалізує wall-clock поля, тож коректно проходить через переведення стрілок
 * (23- чи 25-годинна доба) — на відміну від додавання/віднімання фіксованих `N * 86_400_000` мс,
 * яке в день переведення стрілок зсуває результат на годину. Дослідження W-1 (агент, 27.09.2026)
 * знайшло кілька місць у коді (`PauseRepository.shiftedDayBoundary`, `PatternRepository`'s
 * денний цикл), що саме так рахують "N днів тому" й не покриті тестом на перехід DST — нові
 * `DaySnapshot`/`WeekSnapshot` свідомо цього не повторюють.
 */
fun addDaysMillis(millis: Long, days: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    cal.add(Calendar.DAY_OF_YEAR, days)
    return cal.timeInMillis
}

/** Найближча календарна північ ПІСЛЯ [millis] — північ іншого дня, навіть якщо [millis] уже точна північ. */
fun nextMidnightMillis(millis: Long): Long = addDaysMillis(localStartOfDay(millis), 1)
