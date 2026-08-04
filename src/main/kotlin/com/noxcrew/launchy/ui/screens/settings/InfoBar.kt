package com.noxcrew.launchy.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.screens.main.buttons.InitialProfileButton
import com.noxcrew.launchy.ui.screens.main.buttons.PlayButton
import com.noxcrew.launchy.ui.state.TopBar

@Composable
fun InfoBar(barOnly: Boolean = false) {
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
            modifier = Modifier.padding(6.dp),
        ) {
            if (!barOnly) {
                PlayButton(TopBar, state.mainProfile)
                Spacer(Modifier.width(10.dp))

                ActionButton(
                    shown = !state.minecraftValid,
                    icon = Icons.Rounded.Error,
                    desc = "No minecraft installation found",
                )

                ActionButton(
                    shown = state.mainProfile.instanceId !in state.fabricUpToDate,
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

                val total = state.downloading.values.sumOf { it.bytesDownloaded }
                val time = state.downloading.values.sumOf { it.timeElapsed } / 1000
                val dps = if (time == 0L) 0 else total / time

                Text(
                    text = if (state.downloading.all { it.value.totalBytes == 0L }) {
                        "Preparing to download ${state.downloading.size + state.downloadingConfigs.size} file(s)"
                    } else {
                        "Downloading ${state.downloading.size + state.downloadingConfigs.size} file(s) (${
                            formatBytes(
                                totalBytesDownloaded,
                                totalBytesToDownload
                            )
                        } at ${formatSpeed(dps)})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.failedDownloads.isNotEmpty()) {
                Spacer(Modifier.width(5.dp))
                // Show failed downloads
                Text(
                    text = "Failed downloads: ${state.failedDownloads.size}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!barOnly && !state.hasProfiles) {
                Spacer(Modifier.weight(1f))
                InitialProfileButton()
            }
        }
    }
}

fun formatBytes(bytes: Long, total: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var doubleSize = total.toDouble()
    var downloadSize = bytes.toDouble()
    var unitIndex = 0
    while (doubleSize >= 1024 && unitIndex < units.lastIndex) {
        doubleSize /= 1024
        downloadSize /= 1024
        unitIndex++
    }
    return "${String.format("%.1f", downloadSize)} / ${String.format("%.1f", doubleSize)} ${units[unitIndex]}"
}

fun formatSpeed(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var doubleSize = bytes.toDouble()
    var unitIndex = 0
    while (doubleSize >= 1024 && unitIndex < units.lastIndex) {
        doubleSize /= 1024
        unitIndex++
    }
    return String.format("%.1f %s/s", doubleSize, units[unitIndex])
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
