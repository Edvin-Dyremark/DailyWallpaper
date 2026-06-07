package com.dyremark.dailywallpaper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dyremark.dailywallpaper.domain.SetNextWallpaperUseCase
import com.dyremark.dailywallpaper.domain.WallpaperResult

/** Periodic worker that rotates the wallpaper to a new random photo from the chosen folder. */
class WallpaperWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (
        SetNextWallpaperUseCase.create(applicationContext).invoke()
    ) {
        WallpaperResult.Success -> Result.success()
        // Permanent: nothing to retry until the user fixes settings (also stops a one-off
        // widget tap from looping forever).
        WallpaperResult.NoFolder, WallpaperResult.NoImages -> Result.failure()
        // Transient: a momentary decode/IO/provider glitch — retry so this cycle isn't skipped.
        is WallpaperResult.Error -> Result.retry()
    }
}
