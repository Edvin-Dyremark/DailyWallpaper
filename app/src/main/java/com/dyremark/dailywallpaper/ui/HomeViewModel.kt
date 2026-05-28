package com.dyremark.dailywallpaper.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dyremark.dailywallpaper.data.FolderRepository
import com.dyremark.dailywallpaper.data.SettingsRepository
import com.dyremark.dailywallpaper.data.WallpaperTarget
import com.dyremark.dailywallpaper.domain.SetNextWallpaperUseCase
import com.dyremark.dailywallpaper.work.Scheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val folderUri: String? = null,
    val folderName: String? = null,
    val imageCount: Int? = null,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val intervalMinutes: Int = 60,
    val rotationEnabled: Boolean = false,
    val busy: Boolean = false,
    val status: String? = null,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private val folders = FolderRepository(app)
    private val useCase = SetNextWallpaperUseCase.create(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                val folderChanged = s.folderUri != _uiState.value.folderUri
                _uiState.update {
                    it.copy(
                        folderUri = s.folderUri,
                        target = s.target,
                        intervalMinutes = s.intervalMinutes,
                        rotationEnabled = s.rotationEnabled,
                    )
                }
                if (folderChanged) refreshFolderInfo(s.folderUri)
            }
        }
    }

    fun onFolderPicked(uri: Uri) {
        viewModelScope.launch {
            // Persist read-only access so the folder is remembered across reboots.
            runCatching {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            settings.setFolderUri(uri.toString())
        }
    }

    fun setTarget(target: WallpaperTarget) {
        viewModelScope.launch { settings.setTarget(target) }
    }

    fun setIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            settings.setIntervalMinutes(minutes)
            if (_uiState.value.rotationEnabled) {
                Scheduler.schedule(getApplication(), minutes)
            }
        }
    }

    fun setRotationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setRotationEnabled(enabled)
            if (enabled) {
                Scheduler.schedule(getApplication(), _uiState.value.intervalMinutes)
            } else {
                Scheduler.cancel(getApplication())
            }
        }
    }

    fun setWallpaperNow() {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, status = null) }
            val result = useCase()
            _uiState.update {
                it.copy(
                    busy = false,
                    status = if (result.isSuccess) {
                        "Wallpaper updated"
                    } else {
                        result.exceptionOrNull()?.message ?: "Failed to set wallpaper"
                    },
                )
            }
        }
    }

    fun consumeStatus() {
        _uiState.update { it.copy(status = null) }
    }

    private suspend fun refreshFolderInfo(folderUri: String?) {
        if (folderUri == null) {
            _uiState.update { it.copy(folderName = null, imageCount = null) }
            return
        }
        val uri = Uri.parse(folderUri)
        val name = runCatching { folders.folderName(uri) }.getOrNull()
        _uiState.update { it.copy(folderName = name, imageCount = null) }
        val count = runCatching { folders.imageCount(uri) }.getOrNull()
        _uiState.update { it.copy(imageCount = count) }
    }
}
