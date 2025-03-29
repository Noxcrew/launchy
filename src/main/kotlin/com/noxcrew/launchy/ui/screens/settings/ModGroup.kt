package com.noxcrew.launchy.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChangeCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.data.Group
import com.noxcrew.launchy.data.Mod
import com.noxcrew.launchy.util.Option

@Composable
fun ModGroup(group: Group, mods: Collection<Mod>) {
    var expanded by remember { mutableStateOf(group.startExpanded) }
    val arrowRotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
    val state = LocalLaunchyState

    val modsChanged = mods.any { it in state.queuedDeletions || it in state.queuedDownloads }

    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(2.dp).fillMaxWidth().clickable { expanded = !expanded },
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(40.dp)
            ) {

                ToggleButtons(
                    onSwitch = { option ->
                        if (option == Option.ENABLED)
                            state.profile.modGroups[group]
                                ?.forEach { state.setModEnabled(it, true) }
                        else if (option == Option.DISABLED)
                            state.profile.modGroups[group]
                                ?.forEach { state.setModEnabled(it, false) }
                    },
                    group = group,
                    mods = mods
                )

                Spacer(Modifier.width(10.dp))
                Text(
                    group.name, Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                AnimatedVisibility(modsChanged) {
                    TooltipArea(
                        modifier = Modifier.alpha(0.5f),
                        tooltip = {
                            Text(
                                text = "Mods in this group have changed",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.ChangeCircle, "Updated")
                    }
                }
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.ArrowDropDown, "Show mods", Modifier.rotate(arrowRotationState))
                Spacer(Modifier.width(10.dp))
            }
            AnimatedVisibility(expanded) {
                Column {
                    for (mod in mods) ModInfo(group, mod)
                }
            }
        }
    }
}
