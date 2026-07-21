package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChangeCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.data.Dirs
import com.noxcrew.launchy.data.Profile
import com.noxcrew.launchy.logic.FabricInstaller
import com.noxcrew.launchy.logic.MinecraftDetector
import com.noxcrew.launchy.logic.PrismInstaller
import com.noxcrew.launchy.ui.state.TopBarState
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun PlayButton(
    topBar: TopBarState,
    profile: Profile,
) {
    val state = LocalLaunchyState
    val coroutineScope = rememberCoroutineScope()
    Button(
        enabled = !state.startingLauncher && state.minecraftValid && state.updating == null,
        onClick = {
            if (!profile.valid) {
                state.errorMessage = """
                    An error occurred while loading version information. Please contact an administrator for assistance!
                """.trimIndent()
                return@Button
            }

            if (!state.startingLauncher && state.minecraftValid && state.updating == null) {
                if (profile.instanceId !in state.profilesCreated || profile.instanceId in state.operationsQueued) {
                    coroutineScope.launch {
                        try {
                            var i = 1
                            while (state.profileConfigs[profile.instanceId]!!.let {
                                    it.queuedUpdates.isNotEmpty() ||
                                            it.queuedInstalls.isNotEmpty() ||
                                            it.queuedDeletions.isNotEmpty() ||
                                            !it.isUpToDate ||
                                            !FabricInstaller.isProfileInstalled(Dirs.minecraft, it.profile.instanceId) ||
                                            !PrismInstaller.isProfileInstalled(it.profile) ||
                                            it.installedVersion != it.profile.getVersionId()
                                }) {
                                println("Started update #$i")
                                state.updating = profile.instanceId
                                state.update(profile)
                                state.save()
                                state.updating = null
                                println("Finished update #$i")
                                i++
                            }
                        } catch (x: Throwable) {
                            x.printStackTrace()
                            state.errorMessage = """
                                An error occurred while installing the game, please close any opened instances of Minecraft or the Minecraft Launcher and try again.
                                
                                If the problem persists ask for help on the Discord.
                                """.trimIndent()
                        }
                    }
                } else {
                    // Bump the profile to the top of the list
                    println("Bumping profile in launcher")
                    FabricInstaller.bumpProfile(Dirs.minecraft, profile.instanceId)

                    // Minimize the launcher
                    state.startingLauncher = true
                    topBar.windowState.isMinimized = true

                    coroutineScope.launch {
                        // Run the Minecraft launcher
                        try {
                            val installations = MinecraftDetector.detectInstallations()
                            if (installations.isEmpty()) {
                                state.errorMessage = """
                                    Could not find any Minecraft Launcher installation. Please manually start the Minecraft Launcher.
                                    
                                    If the problem persists ask for help on the Discord.
                                """.trimIndent()

                                topBar.windowState.isMinimized = false
                                state.startingLauncher = false
                            } else {
                                installations.firstOrNull()?.start()

                                // If we want to minimize the window we can disable this, but by default we just close the program
                                // and let the launcher do its thing!
                                exitProcess(0)
                            }
                        } catch (x: Throwable) {
                            x.printStackTrace()

                            state.errorMessage = """
                                An error occurred while opening the launcher. Please manually start the Minecraft Launcher.
                                
                                If the problem persists ask for help on the Discord.
                            """.trimIndent()

                            topBar.windowState.isMinimized = false
                            state.startingLauncher = false
                            return@launch
                        }
                    }
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        @Composable
        fun AnimatedVisibilityScope.button(name: String, icon: ImageVector) {
            Row {
                Icon(icon, name.replace(".", ""))
                Spacer(Modifier.width(5.dp))
                Text(name)
            }
        }

        AnimatedVisibility(!state.minecraftValid) {
            button("Invalid Minecraft", Icons.Rounded.Close)
        }
        AnimatedVisibility(state.minecraftValid) {
            AnimatedVisibility(state.updating == null) {
                AnimatedVisibility(profile.instanceId !in state.profilesCreated) {
                    button("Install", Icons.Rounded.Download)
                }
                AnimatedVisibility(profile.instanceId in state.profilesCreated && profile.instanceId in state.operationsQueued) {
                    button("Update", Icons.Rounded.Download)
                }
                AnimatedVisibility(profile.instanceId in state.profilesCreated && profile.instanceId !in state.operationsQueued) {
                    button("Play", Icons.Rounded.PlayArrow)
                }
            }
            AnimatedVisibility(state.updating == profile.instanceId) {
                AnimatedVisibility(profile.instanceId !in state.profilesCreated) {
                    button("Installing...", Icons.Rounded.Update)
                }
                AnimatedVisibility(profile.instanceId in state.profilesCreated) {
                    button("Updating...", Icons.Rounded.Update)
                }
            }
            AnimatedVisibility(state.updating != null && state.updating != profile.instanceId) {
                button("Waiting", Icons.Rounded.ChangeCircle)
            }
        }
    }
}