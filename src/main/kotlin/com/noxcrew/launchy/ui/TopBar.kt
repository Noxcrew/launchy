package com.noxcrew.launchy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.ui.state.TopBarState

@Composable
fun WindowButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxHeight().width(44.dp),
        contentColor = Color.White,
        color = Color.Transparent
    ) {
        Icon(icon, "", Modifier.padding(10.dp))
    }
}

@Composable
fun AppTopBar(
    state: TopBarState,
    transparent: Boolean,
    showBackButton: Boolean,
    onBackButtonClicked: (() -> Unit),
) = state.windowScope.WindowDraggableArea {
    Box(
        Modifier.fillMaxWidth().height(40.dp)
    ) {
        AnimatedVisibility(
            !transparent,
            enter = slideIn(initialOffset = { IntOffset(0, -40) }),
            exit = slideOut(targetOffset = { IntOffset(0, -40) })
        ) {
            Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxSize()) {}
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Spacer(Modifier.width(5.dp))
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(showBackButton/*, enter = fadeIn(animationSpec = tween(300, 300))*/) {
                    IconButton(onClick = onBackButtonClicked) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back button")
                    }
                    Spacer(Modifier.width(5.dp))
                }
                AnimatedVisibility(!transparent) {
                    Text(
                        "MCC Launcher",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Row {
                WindowButton(Icons.Rounded.HorizontalRule) {
                    state.windowState.isMinimized = true
                }
                WindowButton(Icons.Rounded.CropSquare) {
                    state.toggleMaximized()
                }
                WindowButton(Icons.Rounded.Close) {
                    state.onClose()
                }
            }
        }
    }
}
