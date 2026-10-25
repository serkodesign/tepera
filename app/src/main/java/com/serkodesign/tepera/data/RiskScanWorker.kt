package com.serkodesign.tepera.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.ScanOutcome
import java.util.concurrent.TimeUnit

private const val UNIQUE_NAME = "v3_risk_scan"

/**
 * v3.0 MVP, Етап 2 (п. 9): раз на тиждень оновлює слоти, шукає години ризику й перевіряє зсув відомих вікон.
 * Нічого не показує. Збій читання системних подій — `retry` (WorkManager повторить із затримкою).
 */
class RiskScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = (applicationContext as TeperaApp).riskScanner.scan()
        NudgeScheduler.reschedule(applicationContext)
        return when (outcome) {
            ScanOutcome.Failed -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RiskScanWorker>(7, TimeUnit.DAYS).build()
            )
        }
    }
}
