package com.noxcrew.launchy.ui.screens.main.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.screens.Screen
import com.noxcrew.launchy.ui.screens.openScreen

@Composable
fun ProfileButton(switch: Boolean = false) {
    if (switch) {
        Button(onClick = { openScreen(Screen.Profiles) }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
            Icon(Icons.Rounded.Dataset, "Profiles", modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text("Switch Profiles", fontSize = 12.sp)
        }
    } else {
        Button(onClick = { openScreen(Screen.Profiles) }) {
            Icon(Icons.Rounded.Dataset, "Profiles")
            Spacer(Modifier.width(5.dp))
            Text("Profiles")
        }
    }
}