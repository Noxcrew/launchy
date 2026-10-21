package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.theme.MccButton
import com.noxcrew.launchy.ui.theme.MccButtonColors

@Composable
fun InitialProfileButton() {
    val state = LocalLaunchyState
    MccButton(
        enabled = state.minecraftValid && !state.isDownloading,
        colors = MccButtonColors.Dark,
        onClick = {
            state.initialProfileDialog = true
        }
    ) {
        Icon(Icons.Rounded.Dataset, "Add profile")
    }
}