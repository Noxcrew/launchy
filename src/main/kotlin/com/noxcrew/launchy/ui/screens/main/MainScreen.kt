package com.noxcrew.launchy.ui.screens.main

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.LocalLaunchyState
import com.noxcrew.launchy.ui.screens.main.buttons.PlayButton
import com.noxcrew.launchy.ui.screens.main.buttons.SettingsButton
import com.noxcrew.launchy.ui.state.TopBar
import com.noxcrew.launchy.ui.state.windowScope

@Preview
@Composable
fun MainScreen() {
    val state = LocalLaunchyState

    Box {
        BackgroundImage(windowScope)

        Column(
            modifier =
            Modifier.align(Alignment.Center)
                .heightIn(0.dp, 550.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoLarge(Modifier.weight(3f))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                PlayButton(TopBar)
                Spacer(Modifier.width(10.dp))
                SettingsButton()
            }
        }

        FirstLaunchDialog()
        HandleImportSettings()
        if (state.errorMessage.isNotBlank()) ErrorPopup()
    }
}
