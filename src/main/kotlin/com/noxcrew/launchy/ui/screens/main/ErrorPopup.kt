package com.noxcrew.launchy.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.state.windowScope

@Composable
fun ErrorPopup() {
    val state = LocalLaunchyState
    AnimatedVisibility(
        state.errorMessage.isNotBlank(),
        enter = fadeIn(), exit = fadeOut(),
    ) {
        ErrorPopup(windowScope)
    }
}

@Composable
fun ErrorPopup(
    windowScope: WindowScope,
) {
    val state = LocalLaunchyState

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
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    state.errorMessage = ""
                }) {
                    Text("Dismiss")
                }
            }
        }
    }
}
