package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChangeCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.data.Dirs
import com.noxcrew.launchy.data.Profile
import com.noxcrew.launchy.logger
import com.noxcrew.launchy.logic.FabricInstaller
import com.noxcrew.launchy.logic.MinecraftDetector
import com.noxcrew.launchy.logic.PrismInstaller
import com.noxcrew.launchy.ui.state.TopBarState
import com.noxcrew.launchy.ui.theme.MccButton
import com.noxcrew.launchy.ui.theme.MccButtonColors
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun PlayButton(
    topBar: TopBarState,
    profile: Profile,
    withPackName: Boolean = false,
) {
    val state = LocalLaunchyState
    MccButton(
        enabled = !state.startingLauncher && state.atLeastOneInstallPresent && state.updating == null,
        colors = MccButtonColors.Yellow,
        onClick = {
            if (!profile.valid) {
                state.errorMessage = """
                    An error occurred while loading version information. Please contact an administrator for assistance!
                """.trimIndent()
                return@MccButton
            }

            if (!state.startingLauncher && state.atLeastOneInstallPresent && state.updating == null) {
                if (profile.instanceId !in state.profilesCreated || profile.instanceId in state.operationsQueued) {
                    GlobalScope.launch {
                        try {
                            var i = 1
                            while (state.profileConfigs[profile.instanceId]!!.let {
                                    it.queuedUpdates.isNotEmpty() ||
                                            it.queuedInstalls.isNotEmpty() ||
                                            it.queuedDeletions.isNotEmpty() ||
                                            !it.isUpToDate ||
                                            (state.vanillaMinecraftValid && !FabricInstaller.isProfileInstalled(Dirs.minecraft, it.profile.instanceId)) ||
                                            !PrismInstaller.isProfileInstalled(it.profile) ||
                                            it.installedVersion != it.profile.getVersionId()
                                }) {
                                logger.info("Started update #$i")
                                state.updating = profile.instanceId
                                state.update(profile)
                                state.save()
                                state.updating = null
                                logger.info("Finished update #$i")
                                i++
                            }
                        } catch (x: Throwable) {
                            logger.error("Failed to update game", x)
                            state.errorMessage = """
                                An error occurred while installing the game, please close any opened instances of Minecraft or the Minecraft Launcher and try again.
                                
                                If the problem persists, ask for help on the Discord.
                                """.trimIndent()
                            state.errorStack = x.stackTraceToString()
                        }
                    }
                } else {
                    // Bump the profile to the top of the list
                    logger.info("Bumping profile in launcher")
                    FabricInstaller.bumpProfile(Dirs.minecraft, profile.instanceId)

                    // Minimize the launcher
                    state.startingLauncher = true
                    topBar.windowState.isMinimized = true

                    GlobalScope.launch {
                        // Run the Minecraft launcher
                        try {
                            val installations = MinecraftDetector.detectInstallations()
                            if (installations.isEmpty()) {
                                state.errorMessage = """
                                    Could not find any Minecraft Launcher installation. Please manually start the Minecraft Launcher.
                                    
                                    If the problem persists, ask for help on the Discord.
                                """.trimIndent()

                                topBar.windowState.isMinimized = false
                                state.startingLauncher = false
                            } else {
                                val installation = installations.first()
                                logger.info("Launching ${installation.type}")
                                installation.start()

                                // If we want to minimize the window we can disable this, but by default we just close the program
                                // and let the launcher do its thing!
                                exitProcess(0)
                            }
                        } catch (x: Throwable) {
                            logger.error("Failed to start launcher", x)
                            state.errorMessage = """
                                An error occurred while opening the launcher. Please manually start the Minecraft Launcher.
                                
                                If the problem persists, ask for help on the Discord.
                            """.trimIndent()

                            topBar.windowState.isMinimized = false
                            state.startingLauncher = false
                            return@launch
                        }
                    }
                }
            }
        }
    ) {
        @Composable
        fun AnimatedVisibilityScope.button(name: String, icon: ImageVector, includePackName: Boolean = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Icon(icon, name.replace(".", ""))
                    Spacer(Modifier.width(5.dp))
                    Text(name)
                }
                if (withPackName && includePackName) {
                    Text("${profile.displayName} v${profile.version}",
                        fontStyle = FontStyle.Normal,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        AnimatedVisibility(!state.atLeastOneInstallPresent) {
            button("INVALID MINECRAFT", Icons.Rounded.Close, false)
        }
        AnimatedVisibility(state.atLeastOneInstallPresent) {
            AnimatedVisibility(state.updating == null) {
                AnimatedVisibility(profile.instanceId !in state.profilesCreated) {
                    button("INSTALL", Icons.Rounded.Download)
                }
                AnimatedVisibility(profile.instanceId in state.profilesCreated && profile.instanceId in state.operationsQueued) {
                    button("UPDATE", Icons.Rounded.Download)
                }
                AnimatedVisibility(profile.instanceId in state.profilesCreated && profile.instanceId !in state.operationsQueued) {
                    button("PLAY", Icons.Rounded.PlayArrow)
                }
            }
            AnimatedVisibility(state.updating == profile.instanceId) {
                AnimatedVisibility(profile.instanceId !in state.profilesCreated) {
                    button("INSTALLING...", Icons.Rounded.Update)
                }
                AnimatedVisibility(profile.instanceId in state.profilesCreated) {
                    button("UPDATING...", Icons.Rounded.Update)
                }
            }
            AnimatedVisibility(state.updating != null && state.updating != profile.instanceId) {
                button("WAITING", Icons.Rounded.ChangeCircle)
            }
        }
    }
}