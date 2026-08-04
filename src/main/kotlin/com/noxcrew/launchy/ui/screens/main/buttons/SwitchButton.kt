package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowCircleRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState

@Composable
fun SwitchButton(profileId: String) {
    val state = LocalLaunchyState
    Button(
        enabled = state.updating == null,
        onClick = {
            state.changeProfile(profileId)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row {
            Icon(Icons.Rounded.ArrowCircleRight, "Switch")
            Spacer(Modifier.width(5.dp))
            Text("Switch")
        }
    }
}