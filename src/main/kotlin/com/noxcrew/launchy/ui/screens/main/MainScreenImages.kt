package com.noxcrew.launchy.ui.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope

@Composable
fun BoxScope.BackgroundImage(windowScope: WindowScope) {
    windowScope.WindowDraggableArea {
        Image(
            painter = painterResource("background.png"),
            contentDescription = "Main render",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun LogoLarge(modifier: Modifier) {
    Image(
        painter = painterResource("logo.png"),
        contentDescription = "Logo",
        modifier = Modifier.widthIn(0.dp, 400.dp).then(modifier),
        contentScale = ContentScale.FillWidth
    )
}
