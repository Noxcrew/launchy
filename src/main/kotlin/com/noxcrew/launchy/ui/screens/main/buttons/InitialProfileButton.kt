package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.noxcrew.launchy.LocalLaunchyState

@Composable
fun InitialProfileButton() {
    val state = LocalLaunchyState
    Button(
        enabled = state.minecraftValid && !state.isDownloading,
        onClick = {
            state.initialProfileDialog = true
        }
    ) {
        Icon(Icons.Rounded.Dataset, "Add profile")
    }
}