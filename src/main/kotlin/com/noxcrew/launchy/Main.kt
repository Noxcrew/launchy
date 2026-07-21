package com.noxcrew.launchy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.noxcrew.launchy.data.Config
import com.noxcrew.launchy.data.Dirs
import com.noxcrew.launchy.data.Profile
import com.noxcrew.launchy.logic.LaunchyState
import com.noxcrew.launchy.ui.createColorScheme
import com.noxcrew.launchy.ui.screens.Screens
import com.noxcrew.launchy.ui.state.TopBarProvider
import com.noxcrew.launchy.ui.state.TopBarState
import net.fabricmc.installer.util.Utils
import java.io.InputStream

private val LaunchyStateProvider = compositionLocalOf<LaunchyState> { error("No local versions provided") }

val LocalLaunchyState: LaunchyState
    @Composable
    get() = LaunchyStateProvider.current

@ExperimentalComposeUiApi
fun main() {
    println("MCC Launchy")
    application {
        val windowState = rememberWindowState(placement = WindowPlacement.Floating)
        val launchyState by produceState<LaunchyState?>(null) {
            val config = Config.read()
            val (profiles, errorMessage) = Profile.readAll(config.savedProfiles + config.profileUrl)
            value = LaunchyState(config, profiles, errorMessage)
        }
        val onClose: () -> Unit = {
            exitApplication()
            launchyState?.save()
        }
        Window(
            state = windowState,
            title = "MCC Launcher",
            icon = appIcon,
            onCloseRequest = onClose,
            undecorated = true,
        ) {
            val topBarState = remember { TopBarState(onClose, windowState, this) }
            val ready = launchyState != null
            val scheme = createColorScheme()
            MaterialTheme(colorScheme = scheme) {
                CompositionLocalProvider(TopBarProvider provides topBarState) {
                    Scaffold {
                        AnimatedVisibility(!ready, exit = fadeOut()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Fetching latest mod information...")
                            }
                        }
                        AnimatedVisibility(ready, enter = fadeIn()) {
                            CompositionLocalProvider(
                                LaunchyStateProvider provides launchyState!!,
                            ) {
                                Dirs.createDirs()
                                Screens()
                            }
                        }
                    }
                }
            }
        }
    }
}

private val appIcon: Painter? by lazy {
    val input: InputStream = Utils::class.java.classLoader.getResourceAsStream("icon.png") ?: return@lazy null
    BitmapPainter(input.buffered().use { loadImageBitmap(it) })
}
