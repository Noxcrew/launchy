package com.noxcrew.launchy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.noxcrew.launchy.data.Formats
import com.noxcrew.launchy.data.Profile
import com.noxcrew.launchy.data.ProfileConfig
import com.noxcrew.launchy.logic.LaunchyState
import com.noxcrew.launchy.ui.createColorScheme
import com.noxcrew.launchy.ui.screens.Screens
import com.noxcrew.launchy.ui.state.TopBarProvider
import com.noxcrew.launchy.ui.state.TopBarState
import net.fabricmc.installer.util.Utils
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.relativeTo

private val LaunchyStateProvider = compositionLocalOf<LaunchyState> { error("No local versions provided") }

val LocalLaunchyState: LaunchyState
    @Composable
    get() = LaunchyStateProvider.current

@OptIn(ExperimentalPathApi::class)
@ExperimentalComposeUiApi
fun main() {
    println("MCC Launchy")
    application {
        val windowState = rememberWindowState(placement = WindowPlacement.Floating)
        val launchyState by produceState<LaunchyState?>(null) {
            var config = Config.read()
            var save = false

            if (config.launchyVersion < 3) {
                val (mainProfiles, err) = Profile.readAll(listOf(config.mainProfile))
                val mainProfile = mainProfiles[config.mainProfile]!!

                // Migrate the config from the old version!
                val oldConfig = Formats.yaml.decodeFromStream(ProfileConfig.serializer(), Dirs.configFile.inputStream())

                // Move the old instance folder
                val instanceFolder = Dirs.launchyInstances / mainProfile.instanceId
                Dirs.versionsFolder.deleteRecursively()
                Dirs.configFile.deleteIfExists()
                instanceFolder.createDirectories()
                Files.list(Dirs.mcclaunchy).use { paths ->
                    paths.filter { !it.relativeTo(Dirs.mcclaunchy).startsWith("instances/") }
                        .forEach { child ->
                            Files.move(
                                child,
                                instanceFolder.resolve(child.fileName),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                }

                // Update the config
                config = config.copy(
                    profiles = config.profiles + (mainProfile.instanceId to oldConfig.copy(instanceId = mainProfile.instanceId, profileUrl = config.mainProfile))
                )
                save = true
            }

            val (profiles, errorMessage) = Profile.readAll(config.profiles.values.map { it.profileUrl })
            value = LaunchyState(config, profiles, errorMessage)
            if (save) value?.save()
            value?.verify()
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
