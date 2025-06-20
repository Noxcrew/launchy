package com.noxcrew.launchy.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.screens.main.buttons.PlayButton
import com.noxcrew.launchy.ui.screens.main.buttons.ProfileButton
import com.noxcrew.launchy.ui.state.TopBar

@Composable
fun InfoBar(barOnly: Boolean = false, modifier: Modifier = Modifier) {
    val state = LocalLaunchyState
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.isDownloading) {
            val totalBytesToDownload =
                state.downloading.values.sumOf { it.totalBytes } + state.downloadingConfigs.values.sumOf { it.totalBytes }
            val totalBytesDownloaded =
                state.downloading.values.sumOf { it.bytesDownloaded } + state.downloadingConfigs.values.sumOf { it.bytesDownloaded }
            LinearProgressIndicator(
                progress = totalBytesDownloaded.toFloat() / totalBytesToDownload.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(6.dp)
        ) {
            if (!barOnly) {
                PlayButton(TopBar)
                Spacer(Modifier.width(10.dp))
                ProfileButton()
                Spacer(Modifier.width(10.dp))

                ActionButton(
                    shown = !state.minecraftValid,
                    icon = Icons.Rounded.Error,
                    desc = "No minecraft installation found",
                )

                ActionButton(
                    shown = !state.fabricUpToDate,
                    icon = Icons.Rounded.HistoryEdu,
                    desc = "Will install fabric",
                )
                ActionButton(
                    shown = state.updatesQueued,
                    icon = Icons.Rounded.Update,
                    desc = "Will update",
                    extra = state.queuedUpdates.size.toString(),
                    suffix = "mods"
                )
                ActionButton(
                    shown = state.installsQueued,
                    icon = Icons.Rounded.Download,
                    desc = "Will download",
                    extra = state.queuedInstalls.size.toString(),
                    suffix = "mods"
                )
                ActionButton(
                    shown = state.deletionsQueued,
                    icon = Icons.Rounded.Delete,
                    desc = "Will remove",
                    extra = state.queuedDeletions.size.toString(),
                    suffix = "mods"
                )
                Spacer(Modifier.width(10.dp).weight(1f))
            }

            if (state.isDownloading) {
                // Show download progress
                val totalBytesToDownload =
                    state.downloading.values.sumOf { it.totalBytes } + state.downloadingConfigs.values.sumOf { it.totalBytes }
                val totalBytesDownloaded =
                    state.downloading.values.sumOf { it.bytesDownloaded } + state.downloadingConfigs.values.sumOf { it.bytesDownloaded }
                Text(
                    text = "Downloading ${state.downloading.size + state.downloadingConfigs.size} files (${totalBytesDownloaded / 1000} / ${totalBytesToDownload / 1000} KB)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.failedDownloads.isNotEmpty()) {
                // Show failed downloads
                Text(
                    text = "Failed downloads: ${state.failedDownloads.size}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

//                var path by remember { mutableStateOf("") }
//                Button(onClick = {
//                    path = FileDialog(ComposeWindow()).apply {
////                        setFilenameFilter { dir, name -> name.endsWith(".minecraft") }
//                        isVisible = true
//                    }.directory
//                }) {
//                    Text("File Picker")
//                }
//                Text(path)
        }
    }
}

@Composable
fun ActionButton(shown: Boolean, icon: ImageVector, desc: String, extra: String = "", suffix: String = "") {
    AnimatedVisibility(shown) {
        var toggled by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { toggled = !toggled }) {
                Icon(icon, desc)
            }
            AnimatedVisibility(toggled) {
                Text(desc, Modifier.padding(end = 5.dp))
            }
            Text(extra)
            AnimatedVisibility(toggled) {
                Text(suffix, Modifier.padding(start = 5.dp))
            }
        }
    }
}
