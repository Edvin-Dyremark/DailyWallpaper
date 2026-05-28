package com.dyremark.dailywallpaper.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dyremark.dailywallpaper.data.WallpaperTarget

private data class IntervalOption(val label: String, val minutes: Int)

private val INTERVAL_OPTIONS = listOf(
    IntervalOption("Every 15 minutes", 15),
    IntervalOption("Every 30 minutes", 30),
    IntervalOption("Every hour", 60),
    IntervalOption("Every 6 hours", 360),
    IntervalOption("Every 12 hours", 720),
    IntervalOption("Every day", 1440),
)

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) viewModel.onFolderPicked(uri) }

    LaunchedEffect(state.status) {
        state.status?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatus()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { HomeTopBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FolderCard(
                folderName = state.folderName,
                imageCount = state.imageCount,
                onChooseFolder = { folderPicker.launch(null) },
            )
            TargetCard(target = state.target, onTargetChange = viewModel::setTarget)
            RotationCard(
                intervalMinutes = state.intervalMinutes,
                rotationEnabled = state.rotationEnabled,
                onIntervalChange = viewModel::setIntervalMinutes,
                onRotationEnabledChange = viewModel::setRotationEnabled,
            )
            Button(
                onClick = viewModel::setWallpaperNow,
                enabled = state.folderUri != null && !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.busy) "Setting wallpaper…" else "Set wallpaper now")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar() {
    TopAppBar(title = { Text("Daily Wallpaper") })
}

@Composable
private fun FolderCard(
    folderName: String?,
    imageCount: Int?,
    onChooseFolder: () -> Unit,
) {
    SectionCard(title = "Source folder") {
        if (folderName == null) {
            Text("No folder selected yet.")
        } else {
            Text(folderName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when (imageCount) {
                    null -> "Counting images…"
                    1 -> "1 image"
                    else -> "$imageCount images"
                },
            )
        }
        OutlinedButton(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) {
            Text(if (folderName == null) "Choose folder" else "Change folder")
        }
    }
}

@Composable
private fun TargetCard(
    target: WallpaperTarget,
    onTargetChange: (WallpaperTarget) -> Unit,
) {
    SectionCard(title = "Apply to") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TargetChip("Home", target == WallpaperTarget.HOME) { onTargetChange(WallpaperTarget.HOME) }
            TargetChip("Lock", target == WallpaperTarget.LOCK) { onTargetChange(WallpaperTarget.LOCK) }
            TargetChip("Both", target == WallpaperTarget.BOTH) { onTargetChange(WallpaperTarget.BOTH) }
        }
    }
}

@Composable
private fun TargetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotationCard(
    intervalMinutes: Int,
    rotationEnabled: Boolean,
    onIntervalChange: (Int) -> Unit,
    onRotationEnabledChange: (Boolean) -> Unit,
) {
    SectionCard(title = "Automatic rotation") {
        var expanded by remember { mutableStateOf(false) }
        val selected = INTERVAL_OPTIONS.firstOrNull { it.minutes == intervalMinutes }
            ?: IntervalOption("Every $intervalMinutes minutes", intervalMinutes)

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Interval") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                INTERVAL_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onIntervalChange(option.minutes)
                            expanded = false
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Rotate automatically")
            Switch(checked = rotationEnabled, onCheckedChange = onRotationEnabledChange)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title)
            HorizontalDivider()
            content()
        }
    }
}
