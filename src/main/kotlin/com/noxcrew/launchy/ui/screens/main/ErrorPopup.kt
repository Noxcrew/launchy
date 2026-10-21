package com.noxcrew.launchy.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.state.windowScope
import com.noxcrew.launchy.ui.theme.MccColors

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
    var showErrorStack by remember { mutableStateOf(false) }

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
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (state.errorStack.isNotBlank()) {
                    AnimatedVisibility(showErrorStack) {
                        Text(
                            text = state.errorStack,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.background(MccColors.Dark1, RoundedCornerShape(6.dp)).padding(5.dp),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        state.errorMessage = ""
                        state.errorStack = ""
                    }) {
                        Text("Dismiss")
                    }

                    TextButton(onClick = { showErrorStack = !showErrorStack }) {
                        Text(if (showErrorStack) "Hide error details" else "Show error details")
                    }
                }
            }
        }
    }
}
