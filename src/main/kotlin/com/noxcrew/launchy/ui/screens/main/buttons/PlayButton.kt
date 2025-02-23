package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import kotlinx.coroutines.launch

@Composable
fun PlayButton() {
    val state = LocalLaunchyState
    val coroutineScope = rememberCoroutineScope()
    Button(
        enabled = state.minecraftValid && !state.isDownloading,
        onClick = {
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