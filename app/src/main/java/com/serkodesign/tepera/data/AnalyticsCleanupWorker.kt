package com.serkodesign.tepera.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.analytics.analyticsRetentionCutoff
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private const val NAME = "analytics_cleanup"

/** Раз на добу видаляє денні лічильники аналітики старші за 90 днів. */
class AnalyticsCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as TeperaApp
        return runCatching {
            app.database.dailyEventCounterDao().deleteOlderThan(analyticsRetentionCutoff(LocalDate.now()))
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AnalyticsCleanupWorker>(1, TimeUnit.DAYS).build()
            )
        }
    }
}
