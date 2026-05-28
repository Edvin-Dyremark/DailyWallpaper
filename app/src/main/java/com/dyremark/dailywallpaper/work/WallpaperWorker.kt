package com.dyremark.dailywallpaper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dyremark.dailywallpaper.domain.SetNextWallpaperUseCase

/** Periodic worker that rotates the wallpaper to a new random photo from the chosen folder. */
class WallpaperWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result = SetNextWallpaperUseCase.create(applicationContext).invoke()
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
