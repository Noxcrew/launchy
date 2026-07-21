package com.noxcrew.launchy.ui.screens.profiles

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.data.Profile
import com.noxcrew.launchy.ui.screens.main.buttons.PlayButton
import com.noxcrew.launchy.ui.screens.main.buttons.RemoveProfileButton
import com.noxcrew.launchy.ui.screens.main.buttons.SwitchButton
import com.noxcrew.launchy.ui.state.TopBar

@Composable
fun ProfileBar(url: String, profile: Profile) {
    val state = LocalLaunchyState

    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(0.dp, 10.dp),
        color = when {
            state.mainProfileUrl == url -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(15.dp).fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                profile.info?.icon?.also { icon ->
                    Image(
                        painter = rememberAsyncImagePainter(icon),
                        contentDescription = "Profile Icon",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    profile.displayName, Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                if (url != state.mainProfileUrl) {
                    SwitchButton(url)
                    Spacer(Modifier.size(1.dp))
                }
                PlayButton(TopBar, profile)
                if (url != state.mainProfileUrl) {
                    Spacer(Modifier.size(1.dp))
                    RemoveProfileButton(profile)
                }
            }
            Spacer(Modifier.width(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.width(10.dp))
                Text("Minecraft ${profile.minecraftVersion}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(10.dp))
                Text("${profile.nameToMod.size} mods", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.width(10.dp))
                Text(url.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
