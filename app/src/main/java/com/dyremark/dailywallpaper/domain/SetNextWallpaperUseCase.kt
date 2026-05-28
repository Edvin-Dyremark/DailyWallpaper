package com.dyremark.dailywallpaper.domain

import android.content.Context
import android.net.Uri
import com.dyremark.dailywallpaper.data.FolderRepository
import com.dyremark.dailywallpaper.data.SettingsRepository
import com.dyremark.dailywallpaper.data.WallpaperSetter

/**
 * The single "pick a random photo from the chosen folder and set it as wallpaper" action,
 * shared by the manual button and the background worker.
 */
class SetNextWallpaperUseCase(
    private val settings: SettingsRepository,
    private val folders: FolderRepository,
    private val wallpaperSetter: WallpaperSetter,
) {
    suspend operator fun invoke(): Result<Unit> {
        val current = settings.current()
        val folderUri = current.folderUri
            ?: return Result.failure(IllegalStateException("No folder selected"))

        val tree = Uri.parse(folderUri)
        val exclude = current.lastUri?.let(Uri::parse)
        val image = folders.pickRandomImage(tree, exclude)
            ?: return Result.failure(IllegalStateException("No images found in the selected folder"))

        return wallpaperSetter.setWallpaper(image, current.target)
            .onSuccess { settings.setLastUri(image.toString()) }
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
