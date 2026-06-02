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
        // failure() (not retry()) so a one-off widget/manual run doesn't loop when there's no
        // folder or images; the periodic schedule still fires again next interval regardless.
        return if (result.isSuccess) Result.success() else Result.failure()
    }
}
