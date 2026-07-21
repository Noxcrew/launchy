package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.data.Profile

@Composable
fun RemoveProfileButton(profile: Profile) {
    val state = LocalLaunchyState
    IconButton(
        enabled = state.updating == null,
        modifier = Modifier.size(40.dp),
        onClick = {
            state.removeProfile(profile)
        }
    ) {
        Icon(Icons.Rounded.Close, "Remove")
    }
}