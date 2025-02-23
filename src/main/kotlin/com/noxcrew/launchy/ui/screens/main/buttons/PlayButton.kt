package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import com.noxcrew.launchy.logic.FabricInstaller
import com.noxcrew.launchy.ui.state.TopBarState
import com.noxcrew.launchy.util.OS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayButton(
    topBar: TopBarState,
) {
    val state = LocalLaunchyState
    val coroutineScope = rememberCoroutineScope()
    Button(
        enabled = !state.startingLauncher && state.minecraftValid && !state.isDownloading,
        onClick = {
            if (!state.startingLauncher && state.minecraftValid && !state.isDownloading) {
                if (state.operationsQueued) {
                    coroutineScope.launch {
                        try {
                            state.update()
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
                    FabricInstaller.bumpProfile(Dirs.minecraft, "MC Championship")

                    // Open the launcher, we just try to open both types!
                    println("Starting launcher")

                    // Run the standalone installer which is located at a different path per OS
                    var errors = 0
                    try {
                        val path = when (OS.get()) {
                            OS.WINDOWS -> "${System.getenv()["ProgramFiles(x86)"]}\\Minecraft Launcher\\MinecraftLauncher.exe"
                            OS.MAC -> "/Applications/Minecraft.app/Contents/MacOS/launcher"
                            OS.LINUX -> "minecraft-launcher"
                        }
                        ProcessBuilder(listOf(path)).start()
                    } catch (x: Throwable) {
                        x.printStackTrace()
                        errors++
                    }

                    // If the regular one didn't work, use "minecraft" protocol for UWP Minecraft Launcher
                    if (errors == 1) {
                        try {
                            val command = listOf("powershell.exe", "start", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft")
                            ProcessBuilder(command).start()
                        } catch (x: Throwable) {
                            x.printStackTrace()
                            errors++
                        }
                    }

                    // If both launchers errored we show a warning!
                    if (errors == 2) {
                        state.errorMessage = """
                            An error occurred while opening the launcher. Please manually start the Minecraft Launcher.
                            
                            If the problem persists ask for help on the Discord.
                            """.trimIndent()
                        return@Button
                    }

                    // If we want to minimize the window we can disable this, but by default we just close the program
                    // and let the launcher do its thing!
                    if (true) {
                        exitProcess(0)
                    }

                    state.startingLauncher = true
                    topBar.windowState.isMinimized = true
                    coroutineScope.launch {
                        delay(10.seconds)
                        state.startingLauncher = false
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
        fun AnimatedVisibilityScope.button(name: String, icon: ImageVector, contentDescription: String) {
            Row {
                Icon(icon, contentDescription)
                Spacer(Modifier.width(5.dp))
                Text(name)
            }
        }


        AnimatedVisibility(!state.minecraftValid) {
            button("Invalid Minecraft", Icons.Rounded.Close, "Invalid")
        }
        AnimatedVisibility(state.minecraftValid) {
            AnimatedVisibility(state.isDownloading) {
                AnimatedVisibility(!state.profileCreated) {
                    button("Installing...", Icons.Rounded.Update, "Update")
                }
                AnimatedVisibility(state.profileCreated) {
                    button("Updating...", Icons.Rounded.Update, "Update")
                }
            }
            AnimatedVisibility(!state.isDownloading) {
                AnimatedVisibility(!state.profileCreated && state.operationsQueued) {
                    button("Install", Icons.Rounded.Download, "Download")
                }
                AnimatedVisibility(state.profileCreated && state.operationsQueued) {
                    button("Update", Icons.Rounded.Download, "Download")
                }
                AnimatedVisibility(!state.operationsQueued) {
                    button("Play", Icons.Rounded.PlayArrow, "Play")
                }
            }
        }
    }
}