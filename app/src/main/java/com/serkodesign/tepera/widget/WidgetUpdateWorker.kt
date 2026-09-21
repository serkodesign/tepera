package com.serkodesign.tepera.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.serkodesign.tepera.util.nextDayRolloverMillis
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NAME = "widget_update"

/**
 * FR-4.3: періодичне оновлення віджета (~30 хв) — Glance не має live-потоку даних, тож без
 * цього шкала балансу застигла б до наступного ручного відкриття застосунку/resize віджета.
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetLiveData.forceRefresh()
        TeperaWidget().updateAll(applicationContext)
        TeperaWidget4x2().updateAll(applicationContext)
        TeperaWidget1x1().updateAll(applicationContext)
        TeperaWidget2x1().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

private const val ROLLOVER_WORK_NAME = "widget_day_rollover"

/**
 * Разове оновлення віджета о 01:00 — момент, коли віджет (як і Home) переходить з попередньої доби
 * на нову ([com.serkodesign.tepera.util.DAY_ROLLOVER_HOUR]). Не чекає на періодичні ~30 хв. Після
 * виконання сам планує наступне на завтра. WorkManager не гарантує секундної точності (Doze може
 * зсунути на кілька хвилин) — точний AlarmManager вимагав би окремого дозволу.
 */
class WidgetRolloverWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetLiveData.forceRefresh()
        TeperaWidget().updateAll(applicationContext)
        TeperaWidget4x2().updateAll(applicationContext)
        TeperaWidget1x1().updateAll(applicationContext)
        TeperaWidget2x1().updateAll(applicationContext)
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val delayMillis = (nextDayRolloverMillis() - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<WidgetRolloverWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(ROLLOVER_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
