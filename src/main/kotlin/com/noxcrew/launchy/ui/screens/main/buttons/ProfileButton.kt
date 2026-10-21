package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.ui.screens.Screen
import com.noxcrew.launchy.ui.screens.openScreen
import com.noxcrew.launchy.ui.theme.MccButton
import com.noxcrew.launchy.ui.theme.MccButtonColors

@Composable
fun ProfileButton(switch: Boolean = false) {
    if (switch) {
        MccButton(colors = MccButtonColors.Yellow, onClick = { openScreen(Screen.Profiles) }) {
            Row {
                Icon(Icons.Rounded.Dataset, "Profiles")
                Spacer(Modifier.width(5.dp))
                Text("SWITCH PROFILES")
            }
        }
    } else {
        MccButton(onClick = { openScreen(Screen.Profiles) }) {
            Row {
                Icon(Icons.Rounded.Dataset, "Profiles")
                Spacer(Modifier.width(5.dp))
                Text("PROFILES")
            }
        }
    }
}