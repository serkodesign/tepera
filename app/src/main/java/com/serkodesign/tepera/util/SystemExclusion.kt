package com.serkodesign.tepera.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager

/**
 * Пакети поточного лаунчера (усіх, хто відповідає на CATEGORY_HOME — на випадок кількох
 * встановлених лаунчерів) та ввімкнених клавіатур — системні застосунки, час у яких не є
 * "екранним часом" у побутовому розумінні. Спільне для BalanceRepository (Online-хвилини,
 * FR-3.1) і PatternRepository (тепловий патерн, FR-D.8) — обидва рахують той самий "Online",
 * лише за різні проміжки часу. Не кешується: і лаунчер, і клавіатура можуть змінитись протягом
 * життя процесу, а сам запит — лише пара дешевих системних викликів.
 */
fun systemExclusionPackages(context: Context): Set<String> {
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val launcherPackages = context.packageManager
        .queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
        .map { it.activityInfo.packageName }
        .toSet()

    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val keyboardPackages = imm.enabledInputMethodList.map { it.packageName }.toSet()

    return launcherPackages + keyboardPackages
}
