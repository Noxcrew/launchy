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
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ImportContacts
import androidx.compose.material.icons.rounded.Place
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
fun ProfileButton() {
    val state = LocalLaunchyState
    Button(
        enabled = state.minecraftValid && !state.isDownloading,
        onClick = {
            state.importingProfile = true
        }
    ) {
        Icon(Icons.Rounded.Dataset, "Change Profile")
        Spacer(Modifier.width(5.dp))
        Text("Change Profile")
    }
}