package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.ui.screens.Screen
import com.noxcrew.launchy.ui.screens.screen

@Composable
fun SettingsButton() {
    Button(onClick = { screen = Screen.Settings }) {
        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        Spacer(Modifier.width(5.dp))
        Text("Settings")
    }
}
