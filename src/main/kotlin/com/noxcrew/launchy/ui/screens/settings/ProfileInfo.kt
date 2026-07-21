package com.noxcrew.launchy.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.screens.main.buttons.ProfileButton

@Composable
fun ProfileInfo() {
    val state = LocalLaunchyState

    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(25.dp, 10.dp),
            ) {
                Text(
                    "Current Profile: ",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(5.dp))
                state.profile.info?.icon?.also { icon ->
                    Image(
                        painter = rememberAsyncImagePainter(icon),
                        contentDescription = "Profile Icon",
                        modifier = Modifier.size(25.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    state.profile.info?.name ?: "Unnamed Profile",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.weight(1f))
                ProfileButton(switch = true)
            }
        }
    }
}
