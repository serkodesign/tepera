package com.serkodesign.tepera.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.repository.SlotRecompute
import java.util.concurrent.TimeUnit

private const val PERIODIC_NAME = "v3_usage_slots"
private const val ONCE_NAME = "v3_usage_slots_once"

/**
 * v3.0 MVP, Етап 0/1: раз на годину (і один раз при кожному старті процесу) дозаписує 15-хвилинні слоти часу в
 * застосунках-стрічках. Нічого не показує й нікуди не надсилає. Збій читання подій — `retry`, а не запис нулів.
 */
class UsageSlotWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TeperaApp
        return when (app.usageSlotRepository.recompute()) {
            is SlotRecompute.Failed -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UsageSlotWorker>(1, TimeUnit.HOURS).build()
            )
            wm.enqueueUniqueWork(ONCE_NAME, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<UsageSlotWorker>().build())
        }
    }
}
