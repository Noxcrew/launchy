package com.noxcrew.launchy.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.data.Dirs
import com.noxcrew.launchy.data.Versions
import com.noxcrew.launchy.logic.LaunchyState
import com.noxcrew.launchy.ui.state.windowScope
import kotlinx.coroutines.launch
import kotlin.io.path.div

@Composable
fun ImportProfileDialog() {
    val state = LocalLaunchyState
    AnimatedVisibility(
        state.importingProfile,
        enter = fadeIn(), exit = fadeOut(),
    ) {
        ImportProfileDialog(windowScope)
    }
}

@Composable
fun ImportProfileDialog(
    windowScope: WindowScope,
) {
    val state = LocalLaunchyState
    val coroutineScope = rememberCoroutineScope()

    // Overlay that prevents clicking behind it
    windowScope.WindowDraggableArea {
        Box(Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)).fillMaxSize())
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.widthIn(280.dp, 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Import Profile",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Edit the URL below to the link of the profile you wish to import. This link will be provided to you by an administrator if you need to use this feature.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(24.dp))
                var text by remember { mutableStateOf(TextFieldValue(state.profileUrl)) }
                TextField(
                    text,
                    { newText -> text = newText }
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(onClick = {
                        val newURL = text.text.trim()

                        coroutineScope.launch {
                            // Try to download the new file
                            try {
                                val targetFile = Dirs.tmp / "versions.yml"
                                val newData = Versions.readLatest(newURL, targetFile, ignoreLocal = true)
                                state.changeProfile(newURL, newData)
                                state.importingProfile = false
                            } catch (x: Throwable) {
                                x.printStackTrace()
                                state.errorMessage = "The given URL does not contain a valid profile!"
                                return@launch
                            }
                        }
                    }) {
                        Text("Update")
                    }
                    TextButton(onClick = {
                        state.importingProfile = false
                    }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
