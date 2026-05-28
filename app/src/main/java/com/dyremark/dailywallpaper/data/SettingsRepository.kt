package com.dyremark.dailywallpaper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads and writes the three user settings (plus the last-used image) via DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val FOLDER_URI = stringPreferencesKey("folder_uri")
        val INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
        val TARGET = stringPreferencesKey("target")
        val ROTATION_ENABLED = booleanPreferencesKey("rotation_enabled")
        val LAST_URI = stringPreferencesKey("last_uri")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    /** Snapshot of the current settings, for use off the UI (worker, one-shot actions). */
    suspend fun current(): Settings = settings.first()

    suspend fun setFolderUri(uri: String) = edit { it[Keys.FOLDER_URI] = uri }

    suspend fun setIntervalMinutes(minutes: Int) = edit { it[Keys.INTERVAL_MINUTES] = minutes }

    suspend fun setTarget(target: WallpaperTarget) = edit { it[Keys.TARGET] = target.name }

    suspend fun setRotationEnabled(enabled: Boolean) = edit { it[Keys.ROTATION_ENABLED] = enabled }

    suspend fun setLastUri(uri: String) = edit { it[Keys.LAST_URI] = uri }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun Preferences.toSettings(): Settings = Settings(
        folderUri = this[Keys.FOLDER_URI],
        intervalMinutes = this[Keys.INTERVAL_MINUTES] ?: 60,
        target = this[Keys.TARGET]?.let { runCatching { WallpaperTarget.valueOf(it) }.getOrNull() }
            ?: WallpaperTarget.BOTH,
        rotationEnabled = this[Keys.ROTATION_ENABLED] ?: false,
        lastUri = this[Keys.LAST_URI],
    )
}
