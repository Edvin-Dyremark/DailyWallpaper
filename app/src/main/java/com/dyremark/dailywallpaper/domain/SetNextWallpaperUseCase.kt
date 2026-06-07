package com.dyremark.dailywallpaper.domain

import android.content.Context
import android.net.Uri
import com.dyremark.dailywallpaper.data.FolderRepository
import com.dyremark.dailywallpaper.data.SettingsRepository
import com.dyremark.dailywallpaper.data.WallpaperSetter

/** Outcome of attempting to set the next wallpaper. */
sealed interface WallpaperResult {
    data object Success : WallpaperResult

    /** Permanent: nothing to do until the user changes settings. */
    data object NoFolder : WallpaperResult

    /** Permanent: the chosen folder has no images. */
    data object NoImages : WallpaperResult

    /** Transient: decoding/IO/setBitmap failed and is worth retrying. */
    data class Error(val cause: Throwable) : WallpaperResult
}

/**
 * The single "pick a random photo from the chosen folder and set it as wallpaper" action,
 * shared by the manual button, the widget, and the background worker.
 */
class SetNextWallpaperUseCase(
    private val settings: SettingsRepository,
    private val folders: FolderRepository,
    private val wallpaperSetter: WallpaperSetter,
) {
    suspend operator fun invoke(): WallpaperResult {
        val current = settings.current()
        val folderUri = current.folderUri ?: return WallpaperResult.NoFolder

        val tree = Uri.parse(folderUri)
        val exclude = current.lastUri?.let(Uri::parse)
        val image = folders.pickRandomImage(tree, exclude) ?: return WallpaperResult.NoImages

        return wallpaperSetter.setWallpaper(image, current.target).fold(
            onSuccess = {
                settings.setLastUri(image.toString())
                WallpaperResult.Success
            },
            onFailure = { WallpaperResult.Error(it) },
        )
    }

    companion object {
        /** Builds the use case with the default repository implementations. */
        fun create(context: Context): SetNextWallpaperUseCase {
            val app = context.applicationContext
            return SetNextWallpaperUseCase(
                settings = SettingsRepository(app),
                folders = FolderRepository(app),
                wallpaperSetter = WallpaperSetter(app),
            )
        }
    }
}
