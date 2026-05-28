package com.dyremark.dailywallpaper.data

/** Which screen(s) the wallpaper is applied to. */
enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH,
}

/** User configuration, persisted in DataStore. */
data class Settings(
    val folderUri: String? = null,
    val intervalMinutes: Int = 60,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val rotationEnabled: Boolean = false,
    /** URI of the last image set, so we can avoid showing it twice in a row. */
    val lastUri: String? = null,
)
