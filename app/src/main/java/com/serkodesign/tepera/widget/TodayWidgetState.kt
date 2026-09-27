package com.serkodesign.tepera.widget

import java.util.Calendar

/** W-3 (`CLAUDE-CODE-TASK-widgets.md`): 4 стани віджета "Сьогодні". */
enum class TodayWidgetState { NO_ACCESS, MORNING_QUIET, NIGHT_WINDOW, DAY }

/**
 * Яким саме зі станів "Сьогодні" показати. Порядок перевірки навмисний: доступ > нічне вікно >
 * тихий ранок > звичайний день — без доступу немає що рахувати незалежно від часу доби чи вікна
 * сну, тож ця перевірка перша.
 */
fun resolveTodayWidgetState(
    hasUsageAccess: Boolean,
    isInsideSleepWindow: Boolean,
    gapsEmpty: Boolean,
    nowMillis: Long
): TodayWidgetState {
    if (!hasUsageAccess) return TodayWidgetState.NO_ACCESS
    if (isInsideSleepWindow) return TodayWidgetState.NIGHT_WINDOW
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    if (gapsEmpty && hour in 5..11) return TodayWidgetState.MORNING_QUIET
    return TodayWidgetState.DAY
}
