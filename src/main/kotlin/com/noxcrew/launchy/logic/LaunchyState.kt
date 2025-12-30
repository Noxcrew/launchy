package com.noxcrew.launchy.logic

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.noxcrew.launchy.data.Config
import com.noxcrew.launchy.data.ConfigURL
import com.noxcrew.launchy.data.Dirs
import com.noxcrew.launchy.data.DownloadURL
import com.noxcrew.launchy.data.Group
import com.noxcrew.launchy.data.GroupName
import com.noxcrew.launchy.data.Mod
import com.noxcrew.launchy.data.ModName
import com.noxcrew.launchy.data.Versions
import com.noxcrew.launchy.data.unzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.ByteBinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import java.util.concurrent.CancellationException
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

class LaunchyState(
    // Config should never be mutated unless it also updates UI state
    private val config: Config,
    initialProfile: Versions,
    initialErrorMessage: String,
) {
    val editMutex = Mutex()

    var profile by mutableStateOf(initialProfile)
    var profileUrl by mutableStateOf(config.profileUrl)

    var enabledMods: Set<Mod> by mutableStateOf(emptySet())
    val disabledMods: Set<Mod> by derivedStateOf { profile.nameToMod.values.toSet() - enabledMods }
    var downloadedMods: Iterable<Mod> by mutableStateOf(profile.nameToMod.values.filter { it.isDownloaded })

    val downloadURLs = mutableStateMapOf<Mod, DownloadURL>().apply {
        putAll(
            config.downloads
                .mapNotNull { it.key.toMod()?.to(it.value) }
                .toMap()
        )
    }

    val downloadConfigURLs = mutableStateMapOf<Mod, ConfigURL>().apply {
        putAll(
            config.configs
                .mapNotNull { it.key.toMod()?.to(it.value) }
                .toMap()
        )
    }

    var installedFabricVersion by mutableStateOf(config.installedFabricVersion)
    var installedMinecraftVersion by mutableStateOf(config.installedMinecraftVersion)
    var installedVersion by mutableStateOf(config.installedVersion)
    var installedMods by mutableStateOf(config.installed)

    init {
        updateEnabled()
    }

    val upToDateMods by derivedStateOf {
        enabledMods.filter { it in downloadedMods && it in downloadURLs && downloadURLs[it] == it.url }
    }

    val upToDateConfigs by derivedStateOf {
        enabledMods.filter { it in downloadConfigURLs && downloadConfigURLs[it] == it.configUrl }
    }

    val enabledModsWithConfig by derivedStateOf {
        enabledMods.filter { it.configUrl != "" }
    }

    val queuedDownloads by derivedStateOf { (enabledMods - upToDateMods.toSet()) + (enabledModsWithConfig - upToDateConfigs.toSet()) }
    val queuedUpdates by derivedStateOf { queuedDownloads.filter { it in downloadedMods }.toSet() }
    val queuedInstalls by derivedStateOf { queuedDownloads - queuedUpdates }
    val queuedDeletions: List<ModName> by derivedStateOf {
        disabledMods.filter { it in downloadedMods }.map { it.name } +
                (installedMods - profile.nameToMod.keys.toSet())
    }

    var enabledConfigs: Set<Mod> by mutableStateOf(config.toggledConfigs.mapNotNull { it.toMod() }.toSet())

    init {
        // trigger update incase we have dependencies
        enabledMods.forEach { setModEnabled(it, true, save = false) }
    }

    var updating by mutableStateOf(false)
    val downloading = mutableStateMapOf<Mod, Progress>()
    val downloadingConfigs = mutableStateMapOf<Mod, Progress>()
    val isDownloading by derivedStateOf { downloading.isNotEmpty() || downloadingConfigs.isNotEmpty() }
    var failedDownloads by mutableStateOf(emptySet<Mod>())

    // Caclculate the speed of the download
    val downloadSpeed by derivedStateOf {
        val total = downloading.values.sumOf { it.bytesDownloaded }
        val time = downloading.values.sumOf { it.timeElapsed }
        if (time == 0L) 0 else total / time
    }

    fun isDownloading(mod: Mod) = downloading[mod] != null || downloadingConfigs[mod] != null

    val minecraftValid = (Dirs.minecraft / "launcher_profiles.json").exists()
    var profileCreated by mutableStateOf(
        minecraftValid && FabricInstaller.isProfileInstalled(
            Dirs.minecraft,
            "MC Championship"
        )
    )
    val fabricUpToDate by derivedStateOf {
        profileCreated &&
                installedMinecraftVersion == profile.minecraftVersion &&
                installedFabricVersion == profile.fabricVersion
    }
    val profileUpToDate by derivedStateOf {
        installedVersion == profile.getVersionId(config.launchyVersion)
    }

    val updatesQueued by derivedStateOf { queuedUpdates.isNotEmpty() }
    val installsQueued by derivedStateOf { queuedInstalls.isNotEmpty() }
    val deletionsQueued by derivedStateOf { queuedDeletions.isNotEmpty() }
    val operationsQueued by derivedStateOf { updatesQueued || installsQueued || deletionsQueued || !fabricUpToDate || !profileUpToDate }

    var errorMessage by mutableStateOf(initialErrorMessage)
    var importingProfile by mutableStateOf(false)
    var startingLauncher by mutableStateOf(false)

    // If any state is true, we consider import handled and move on
    var handledImportOptions by mutableStateOf(
        config.handledImportOptions ||
                (Dirs.mcclaunchy / "options.txt").exists() ||
                !minecraftValid
    )

    var handledFirstLaunch by mutableStateOf(config.handledFirstLaunch)

    fun setModEnabled(mod: Mod, enabled: Boolean, save: Boolean = true) {
        if (enabled) {
            enabledMods += mod
            enabledMods.filter { it.name in mod.incompatibleWith || it.incompatibleWith.contains(mod.name) }
                .forEach { setModEnabled(it, false, save = false) }
            disabledMods.filter { it.name in mod.requires }.forEach { setModEnabled(it, true, save = false) }
        } else {
            enabledMods -= mod
            // if a mod is disabled, disable all mods that depend on it
            enabledMods.filter { it.requires.contains(mod.name) }.forEach { setModEnabled(it, false, save = false) }
            // if a mod is disabled, and the dependency is only used by this mod, disable the dependency too, unless it's not marked as a dependency
            enabledMods.filter { dep ->
                mod.requires.contains(dep.name)  // if the mod depends on this dependency
                        && dep.dependency // if the dependency is marked as a dependency
                        && enabledMods.none { it.requires.contains(dep.name) }  // and no other mod depends on this dependency
                        && !profile.modGroups.filterValues { it.contains(dep) }.keys.any { it.forceEnabled } // and the group the dependency is in is not force enabled
            }.forEach { setModEnabled(it, false, save = false) }
        }
        setModConfigEnabled(mod, enabled)
        if (save) save()
    }

    fun setModConfigEnabled(mod: Mod, enabled: Boolean) {
        enabledConfigs = if (mod.configUrl.isNotBlank() && enabled) enabledConfigs.plus(mod) else enabledConfigs.minus(mod)
    }

    suspend fun update() = coroutineScope {
        if (!profileUpToDate || !fabricUpToDate)
            installFabric()
        updateServers()
        for (mod in queuedDownloads) {
            launch(Dispatchers.IO) {
                download(mod)
            }
        }
        for (mod in queuedDeletions) {
            launch(Dispatchers.IO) {
                try {
                    println("Starting deletion of $mod")
                    val file = Dirs.mods / "${mod}.jar"
                    file.deleteIfExists()
                    editMutex.withLock {
                        downloadedMods = downloadedMods.filter { it.name != mod }
                        installedMods = installedMods.minus(mod)
                    }
                    println("Successfully deleted $mod")
                } catch (e: FileSystemException) {
                    // Ignore file system exceptions
                }
            }
        }
    }

    fun updateServers() {
        val serverFile = Dirs.mcclaunchy / "servers.dat"
        val file = if (serverFile.exists()) {
            BinaryTagIO.unlimitedReader().read(serverFile, BinaryTagIO.Compression.NONE)
        } else {
            CompoundBinaryTag.empty()
        }
        val servers = file.getList("servers", ListBinaryTag.empty())

        // Determine what servers to add
        val intended = profile.servers

        // Check if all intended servers are present
        val existing = intended.associateWith { server ->
            servers.firstOrNull { (it as? CompoundBinaryTag)?.getString("ip") == server.ip }
        }
        if (existing.values.none { it == null }) return

        // Put all of our servers at the top whenever we make edits
        var newServers = ListBinaryTag.empty()
        for ((server, current) in existing) {
            if (current == null) {
                newServers = newServers.add(
                    CompoundBinaryTag.from(
                        mapOf(
                            "acceptTextures" to ByteBinaryTag.ONE,
                            "hidden" to ByteBinaryTag.ZERO,
                            "name" to StringBinaryTag.stringBinaryTag(server.name),
                            "ip" to StringBinaryTag.stringBinaryTag(server.ip)
                        )
                    )
                )
            } else {
                newServers = newServers.add(current)
            }
        }

        // Add back the original server list at the bottom
        val serverIps = intended.map { it.ip }
        for (oldServer in servers) {
            if ((oldServer as? CompoundBinaryTag)?.getString("ip") !in serverIps) {
                newServers = newServers.add(oldServer)
            }
        }

        // Update the file
        BinaryTagIO.writer().write(file.put("servers", newServers), serverFile, BinaryTagIO.Compression.NONE)
    }

    @OptIn(ExperimentalPathApi::class)
    fun installFabric() {
        val versionId = profile.getVersionId(config.launchyVersion)
        FabricInstaller.installToLauncher(
            Dirs.minecraft,
            Dirs.mcclaunchy,
            "MC Championship",
            profile.minecraftVersion,
            profile.fabricVersion,
            versionId,
        )
        println("Finished installing profile")
        profileCreated = true
        installedFabricVersion = "Installing..."
        installedFabricVersion = profile.fabricVersion
        installedMinecraftVersion = "Installing..."
        installedMinecraftVersion = profile.minecraftVersion
        installedVersion = "Installing..."
        installedVersion = versionId

        // Move all mods into a separate folder so the user does not
        // get confused when a mod for an older version stops working
        val intendedMods = profile.nameToMod.values.map { it.file }
        Dirs.mods.walk().forEach { file ->
            // Ignore directories
            if (file.isDirectory()) return@forEach

            // We can ignore mods that we intended to have!
            if (file in intendedMods) return@forEach

            // Create the target folder
            if (!Dirs.previousMods.exists()) {
                Dirs.previousMods.createDirectories()
            }

            // Copy the mod across to the previous mods folder
            file.copyTo(Dirs.previousMods.resolve(Dirs.mods.relativize(file)), overwrite = true)
            file.deleteIfExists()
        }
    }

    suspend fun download(mod: Mod) {
        runCatching {
            if (mod !in upToDateMods) {
                try {
                    println("Starting download of ${mod.name}")
                    downloading[mod] = Progress(0, 0, 0) // set progress to 0
                    Downloader.download(url = mod.url, writeTo = mod.file) progress@{
                        downloading[mod] = it
                    }
                    editMutex.withLock {
                        downloadURLs[mod] = mod.url
                        installedMods = installedMods.plus(mod.name)
                        if (mod.isDownloaded) {
                            downloadedMods += mod
                        }
                    }
                    println("Successfully downloaded ${mod.name}")
                } catch (ex: CancellationException) {
                    throw ex // Must let the CancellationException propagate
                } catch (e: Exception) {
                    println("Failed to download ${mod.name}")
                    e.printStackTrace()
                    editMutex.withLock {
                        failedDownloads += mod
                    }
                } finally {
                    println("Finished download of ${mod.name}")
                    editMutex.withLock {
                        downloading -= mod
                    }
                }
            }

            if (mod.configUrl.isNotBlank() && (mod in enabledConfigs) && mod !in upToDateConfigs) {
                try {
                    println("Starting download of ${mod.name} config")
                    downloadingConfigs[mod] = Progress(0, 0, 0) // set progress to 0
                    Downloader.download(url = mod.configUrl, writeTo = mod.config) progress@{
                        downloadingConfigs[mod] = it
                    }
                    editMutex.withLock {
                        downloadConfigURLs[mod] = mod.configUrl
                    }
                    unzip(mod.config.toFile(), Dirs.mcclaunchy.toString())
                    mod.config.toFile().delete()
                    println("Successfully downloaded ${mod.name} config")
                } catch (ex: CancellationException) {
                    throw ex // Must let the CancellationException propagate
                } catch (e: Exception) {
                    println("Failed to download ${mod.name} config")
                    editMutex.withLock {
                        failedDownloads += mod
                    }
                    e.printStackTrace()
                } finally {
                    println("Finished download of ${mod.name} config")
                    editMutex.withLock {
                        downloadingConfigs -= mod
                    }
                }
            }
        }.onFailure {
            if (it !is CancellationException) {
                it.printStackTrace()
            }
//            Badge {
//                Text("Failed to download ${mod.name}: ${it.localizedMessage}!"/*, "OK"*/)
//            }
//            scaffoldState.snackbarHostState.showSnackbar(
//                "Failed to download ${mod.name}: ${it.localizedMessage}!", "OK"
//            )
        }
    }

    fun ModName.toMod(): Mod? = profile.nameToMod[this]
    fun GroupName.toGroup(): Group? = profile.nameToGroup[this]

    val Mod.file get() = Dirs.mods / "${name}.jar"
    val Mod.config get() = Dirs.tmp / "${name}-config.zip"
    val Mod.isDownloaded get() = file.exists()

    fun changeProfile(url: String, versions: Versions) {
        profileUrl = url
        profile = versions
        downloadedMods = profile.nameToMod.values.filter { it.isDownloaded }
        updateEnabled()
        save()
    }

    private fun updateEnabled() {
        val defaultEnabled = profile.groups
            .filter { it.enabledByDefault }
            .map { it.name } - config.seenGroups
        val fullEnabled = config.fullEnabledGroups
        val forceEnabled = profile.groups.filter { it.forceEnabled }.map { it.name }
        val forceDisabled = profile.groups.filter { it.forceDisabled }
        val fullDisabled = config.fullDisabledGroups

        val enabled = config.toggledMods.mapNotNull { it.toMod() }.toMutableSet()
        enabled.addAll(
            ((fullEnabled + defaultEnabled + forceEnabled).toSet())
                .mapNotNull { it.toGroup() }
                .mapNotNull { profile.modGroups[it] }.flatten()
        )
        enabled.removeAll((forceDisabled + fullDisabled).toSet().mapNotNull { profile.modGroups[it] }.flatten().toSet())
        enabledMods = enabled
    }

    fun save() {
        config.copy(
            fullEnabledGroups = profile.modGroups
                .filter { enabledMods.containsAll(it.value) }.keys
                .map { it.name }.toSet(),
            toggledMods = enabledMods.mapTo(mutableSetOf()) { it.name },
            toggledConfigs = enabledConfigs.mapTo(mutableSetOf()) { it.name } + enabledMods.filter { it.forceConfigDownload }
                .mapTo(mutableSetOf()) { it.name },
            downloads = downloadURLs.mapKeys { it.key.name },
            configs = downloadConfigURLs.mapKeys { it.key.name },
            seenGroups = profile.groups.map { it.name }.toSet(),
            installed = installedMods.toSet(),
            installedFabricVersion = installedFabricVersion,
            installedMinecraftVersion = installedMinecraftVersion,
            installedVersion = installedVersion,
            handledImportOptions = handledImportOptions,
            handledFirstLaunch = handledFirstLaunch,
            profileUrl = profileUrl,
        ).save()
    }
}
