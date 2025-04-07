package com.noxcrew.launchy.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.data.Group
import com.noxcrew.launchy.data.Mod
import com.noxcrew.launchy.logic.LaunchyState
import com.noxcrew.launchy.util.OS
import java.awt.Desktop
import java.net.URI

object Browser {
    val desktop = Desktop.getDesktop()
    fun browse(url: String, state: LaunchyState) {
        // On MacOS use the open command line argument
        if (OS.get() == OS.MAC) {
            Runtime.getRuntime().exec(arrayOf("open", url))
            return
        }

        // On other operating systems we use the desktop browse functionality
        if (Desktop.isDesktopSupported()) {
            synchronized(desktop) {
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI.create(url))
                    return
                }
            }
        }
        state.errorMessage = "Couldn't open a browser window, please manually visit:\n$url"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModInfo(group: Group, mod: Mod) {
    val state = LocalLaunchyState
    val modEnabled by derivedStateOf { mod in state.enabledMods }
    val configEnabled by derivedStateOf { mod in state.enabledConfigs }
    var configExpanded by remember { mutableStateOf(false) }
    val configTabState by animateFloatAsState(targetValue = if (configExpanded) 180f else 0f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!group.forceEnabled && !group.forceDisabled) state.setModEnabled(mod, !modEnabled) },
        color = when (mod) {
            in state.failedDownloads -> MaterialTheme.colorScheme.error
            in state.queuedDeletions -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            in state.queuedInstalls -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)//Color(105, 240, 174, alpha = 25)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        if (state.isDownloading(mod)) {
            val downloaded =
                ((state.downloading[mod]?.bytesDownloaded ?: 0L) + (state.downloadingConfigs[mod]?.bytesDownloaded
                    ?: 0L)).toFloat()
            val total = ((state.downloading[mod]?.totalBytes ?: 0L) + (state.downloadingConfigs[mod]?.totalBytes
                ?: 0L)).toFloat()
            LinearProgressIndicator(
                progress = downloaded / total,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Column(Modifier.padding(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Checkbox(
                    enabled = !group.forceEnabled && !group.forceDisabled,
                    checked = modEnabled,
                    onCheckedChange = { state.setModEnabled(mod, !modEnabled) }
                )

                Row(Modifier.weight(6f)) {
                    val displayedName = if (mod.displayName.isEmpty()) mod.name else mod.displayName
                    Text(displayedName, style = MaterialTheme.typography.bodyLarge)
                    // build list of mods that are incompatible with this mod
                    val incompatibleMods = state.profile.modGroups.flatMap { it.value }
                        .filter { it.incompatibleWith.contains(mod.name) || mod.incompatibleWith.contains(it.name) }
                        .map { it.name }
                    if (mod.requires.isNotEmpty() || incompatibleMods.isNotEmpty()) {
                        TooltipArea(
                            modifier = Modifier.alpha(0.5f),
                            tooltip = {
                                Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                                    if (mod.requires.isNotEmpty() && incompatibleMods.isNotEmpty()) {
                                        Text(
                                            text = "Incompatible with: ${incompatibleMods.joinToString()}\nRequires: ${mod.requires.joinToString()}",
                                            modifier = Modifier.padding(4.dp),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    } else {
                                        if (mod.requires.isNotEmpty()) {
                                            Text(
                                                text = "Requires: ${mod.requires.joinToString()}",
                                                modifier = Modifier.padding(4.dp),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        if (incompatibleMods.isNotEmpty()) {
                                            Text(
                                                text = "Incompatible with: ${incompatibleMods.joinToString()}",
                                                modifier = Modifier.padding(4.dp),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Requires",
                                modifier = Modifier.scale(0.75f)
                            )
                        }
                    }
                }
                Text(
                    mod.desc,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.5f)
                )
                if (mod.configUrl.isNotEmpty()) {
                    TooltipArea(
                        modifier = Modifier.alpha(0.5f),
                        tooltip = {
                            Text(
                                text = "Config",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "ConfigTab",
                            modifier = Modifier
                                .scale(0.75f)
                                .rotate(configTabState)
                                .clickable { configExpanded = !configExpanded }
                        )
                    }
                }
                if (mod.homepage.isNotEmpty()) {
                    TooltipArea(
                        modifier = Modifier.alpha(0.5f),
                        tooltip = {
                            Text(
                                text = "Open Homepage",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    ) {
                        IconButton(
                            modifier = Modifier.alpha(0.5f),
                            onClick = { Browser.browse(mod.homepage, state) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Homepage"
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(configExpanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!mod.forceConfigDownload) state.setModConfigEnabled(mod, !configEnabled)
                    }.fillMaxWidth()
                ) {
                    Spacer(Modifier.width(20.dp))
                    Checkbox(
                        checked = configEnabled || mod.forceConfigDownload,
                        onCheckedChange = {
                            if (!mod.forceConfigDownload) state.setModConfigEnabled(mod, !configEnabled)
                        },
                        enabled = !mod.forceConfigDownload,
                    )
                    Column {
                        Text(
                            "Download our recommended configuration",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (mod.configDesc.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                mod.configDesc,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.alpha(0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
