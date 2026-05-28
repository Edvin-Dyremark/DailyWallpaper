package com.dyremark.dailywallpaper.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules and cancels the periodic wallpaper rotation. */
object Scheduler {

    private const val WORK_NAME = "wallpaper_rotation"

    /** WorkManager refuses periodic intervals shorter than this. */
    const val MIN_INTERVAL_MINUTES = 15

    fun schedule(context: Context, intervalMinutes: Int) {
        val minutes = intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES).toLong()
        val request = PeriodicWorkRequestBuilder<WallpaperWorker>(minutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // UPDATE keeps the existing schedule's history but applies the new interval.
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
