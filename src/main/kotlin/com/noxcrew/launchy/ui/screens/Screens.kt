package com.noxcrew.launchy.ui.screens

import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.noxcrew.launchy.ui.AppTopBar
import com.noxcrew.launchy.ui.screens.main.MainScreen
import com.noxcrew.launchy.ui.screens.profiles.ProfilesScreen
import com.noxcrew.launchy.ui.screens.settings.SettingsScreen
import com.noxcrew.launchy.ui.state.TopBar
import java.util.Stack

sealed class Screen(val transparentTopBar: Boolean = false) {
    object Default : Screen(transparentTopBar = true)
    object Settings : Screen()
    object Profiles : Screen()
}

var screen: Screen by mutableStateOf(Screen.Default)
val previousScreens = Stack<Screen>()

fun openScreen(newScreen: Screen) {
    previousScreens += screen
    screen = newScreen
}

fun popScreen() {
    screen = previousScreens.pop() ?: Screen.Default
}

@Composable
@Preview
fun Screens() {
    TransitionFade(screen == Screen.Default) {
        MainScreen()
    }

    TranslucentTopBar(screen) {
        TransitionSlideUp(screen == Screen.Settings) {
            SettingsScreen()
        }
        TransitionSlideUp(screen == Screen.Profiles) {
            ProfilesScreen()
        }
    }

    AppTopBar(
        TopBar,
        screen.transparentTopBar,
        showBackButton = screen != Screen.Default,
        onBackButtonClicked = { popScreen() }
    )
}

@Composable
fun TranslucentTopBar(currentScreen: Screen, content: @Composable () -> Unit) {
    Column {
        AnimatedVisibility(!currentScreen.transparentTopBar, enter = fadeIn(), exit = fadeOut()) {
            Spacer(Modifier.height(40.dp))
        }
        content()
    }
}

@Composable
fun TransitionFade(enabled: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(enabled, enter = fadeIn(), exit = fadeOut()) {
        content()
    }
}

@Composable
fun TransitionSlideUp(enabled: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        enabled,
        enter = fadeIn() + slideIn(initialOffset = { IntOffset(0, 100) }),
        exit = fadeOut() + slideOut(targetOffset = { IntOffset(0, 100) }),
    ) {
        content()
    }
}

