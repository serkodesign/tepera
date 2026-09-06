package com.serkodesign.tepera.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Один застосунок для Exclusion List (FR-3.5) — назва й іконка, дружні до користувача. */
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val resolvedIcon: Drawable?
)

/**
 * СПАЙК (ROADMAP.md, Фаза 2 / SRS розділ 10, ризики) — ВИСНОВОК:
 *
 * queryUsageStats() повертає ім'я пакета навіть для застосунків, невидимих через звичайний
 * PackageManager (сам UsageStatsManager не підпорядковується package visibility filtering,
 * Android 11+ — він працює через окремий AppOps-грант PACKAGE_USAGE_STATS). АЛЕ показати
 * людську назву й іконку без додаткової видимості не вийшло: на реальному пристрої (Samsung
 * S23, Android 16, без будь-яких <queries>) 226 із 580 пакетів, побачених за 30 днів, НЕ
 * резолвились через getApplicationInfo() — включно з реальним застосунком користувача
 * (Pocket Casts), а переважна частина решти 580 — системний "шум" (drivers, RRO-оверлеї
 * тощо), який годі показувати в Exclusion List.
 *
 * Рішення — НЕ QUERY_ALL_PACKAGES (другий чутливий дозвіл, ризик Google Play review), а
 * оголошення видимості лише застосунків з launcher-іконкою через <queries> у маніфесті:
 * це (а) відсікає системний шум — жоден non-launchable сервіс/драйвер більше не потрапляє
 * у список, і (б) повертає видимість Pocket Casts та подібних реальних застосунків. Після
 * цієї зміни залишкова кількість unresolved — уже не показник помилки, а очікувані рідкісні
 * застосунки без launcher-активності серед реально використаних.
 */
class InstalledAppsProvider(private val context: Context) {

    suspend fun listUsedApps(sinceDays: Int = 30): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - sinceDays * 24L * 60 * 60 * 1000

        val usedPackageNames = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            .map { it.packageName }
            .toSet()
            .filterNot { it == context.packageName }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val launchablePackageNames = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()

        val relevantPackageNames = usedPackageNames.filter { it in launchablePackageNames }

        var nameResolutionFailureCount = 0

        val apps = relevantPackageNames.map { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                InstalledAppInfo(
                    packageName = pkg,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    resolvedIcon = pm.getApplicationIcon(appInfo)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                nameResolutionFailureCount++
                InstalledAppInfo(packageName = pkg, label = pkg, resolvedIcon = null)
            }
        }.sortedBy { it.label.lowercase() }

        Log.d(
            "InstalledAppsProvider",
            "Spike (FR-3.5): ${usedPackageNames.size} used packages seen via UsageStatsManager, " +
                "${apps.size} launchable and relevant for Exclusion List, " +
                "$nameResolutionFailureCount still unresolved via PackageManager despite <queries>."
        )

        apps
    }
}
