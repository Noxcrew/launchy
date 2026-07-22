package com.noxcrew.launchy.logic

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.noxcrew.launchy.data.Config
import com.noxcrew.launchy.data.Dirs
import com.noxcrew.launchy.data.Mod
import com.noxcrew.launchy.data.ModName
import com.noxcrew.launchy.data.Profile
import com.noxcrew.launchy.data.ProfileConfig
import com.noxcrew.launchy.data.unzip
import com.noxcrew.launchy.ui.screens.Screen
import com.noxcrew.launchy.ui.screens.openScreen
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
    initialProfiles: Map<String, Profile>,
    initialErrorMessage: String,
) {
    val editMutex = Mutex()

    var allProfilesByUrl: Map<String, Profile> by mutableStateOf(initialProfiles)
    var profileConfigs by mutableStateOf(buildMap {
        config.profiles.forEach { (key, value) ->
            val profile = allProfilesByUrl[value.profileUrl]!!
            put(key, value.copy(_profile = profile, instanceId = profile.instanceId))
        }
        initialProfiles
            .filter { it.value.instanceId !in this }
            .forEach { (url, profile) ->
                put(profile.instanceId, ProfileConfig(profile, profile.instanceId, url))
            }
        initialProfiles[config.mainProfile]?.takeUnless { it.instanceId in this }?.also {
            put(it.instanceId, ProfileConfig(it, it.instanceId, config.mainProfile))
        }
    })

    var mainProfileUrl by mutableStateOf(config.mainProfile)
    var mainProfile by mutableStateOf(initialProfiles[mainProfileUrl]!!)
    var mainProfileConfig by mutableStateOf(profileConfigs[mainProfile.instanceId]!!)

    val enabledMods: Set<Mod> by derivedStateOf { mainProfileConfig.enabledMods }
    val downloadedMods: List<Mod> by derivedStateOf { mainProfileConfig.downloadedMods }

    val queuedDownloads by derivedStateOf { mainProfileConfig.queuedDownloads }
    val queuedUpdates by derivedStateOf { queuedDownloads.filter { it in downloadedMods }.toSet() }
    val queuedInstalls by derivedStateOf { queuedDownloads - queuedUpdates }
    val queuedDeletions: List<ModName> by derivedStateOf { mainProfileConfig.queuedDeletions }

    var updating by mutableStateOf<String?>(null)
    val downloading = mutableStateMapOf<Mod, Progress>()
    val downloadingConfigs = mutableStateMapOf<Mod, Progress>()
    val isDownloading by derivedStateOf { downloading.isNotEmpty() || downloadingConfigs.isNotEmpty() }
    var failedDownloads by mutableStateOf(emptySet<Mod>())

    fun isDownloading(mod: Mod) = downloading[mod] != null || downloadingConfigs[mod] != null

    val minecraftValid = (Dirs.minecraft / "launcher_profiles.json").exists()
    val profilesCreated by derivedStateOf {
        profileConfigs.values.filter {
            minecraftValid &&
                    FabricInstaller.isProfileInstalled(Dirs.minecraft, it.instanceId) &&
                    PrismInstaller.isProfileInstalled(it.profile)
        }.map { it.instanceId }
    }
    val fabricUpToDate by derivedStateOf {
        profileConfigs.values.filter { it.profile.instanceId in profilesCreated && it.isUpToDate }.map { it.instanceId }
    }
    val profileUpToDate by derivedStateOf {
        profileConfigs.values.filter {
            it.installedVersion == it.profile.getVersionId()
        }.map { it.instanceId }
    }

    val updatesQueued by derivedStateOf { queuedUpdates.isNotEmpty() }
    val installsQueued by derivedStateOf { queuedInstalls.isNotEmpty() }
    val deletionsQueued by derivedStateOf { queuedDeletions.isNotEmpty() }
    val operationsQueued by derivedStateOf {
        profileConfigs.values.filter {
            it.queuedUpdates.isNotEmpty() || it.queuedInstalls.isNotEmpty() || it.queuedDeletions.isNotEmpty() || it.instanceId !in fabricUpToDate || it.instanceId !in profileUpToDate
        }.map { it.instanceId }
    }

    var errorMessage by mutableStateOf(initialErrorMessage)
    var initialProfileDialog by mutableStateOf(false)
    var startingLauncher by mutableStateOf(false)

    val hasProfiles by derivedStateOf { allProfilesByUrl.size > 1 }

    var handledFirstLaunch by mutableStateOf(config.handledFirstLaunch)

    var currentEnabledCache: MutableSet<Mod>? = null

    fun verify() {
        val save = currentEnabledCache == null
        if (currentEnabledCache == null) {
            currentEnabledCache = enabledMods.toMutableSet()
        }
        enabledMods.forEach { setModEnabled(it, true) }
        if (save) {
            mainProfileConfig = mainProfileConfig.copy(
                fullEnabledGroups = mainProfile.modGroups
                    .filter { currentEnabledCache!!.containsAll(it.value) }.keys
                    .map { it.name }.toSet(),
                toggledMods = currentEnabledCache!!.mapTo(mutableSetOf()) { it.name },
                updateId = mainProfileConfig.updateId + 1,
            )
            currentEnabledCache = null
            profileConfigs = profileConfigs.plus(mainProfile.instanceId to mainProfileConfig)
            save()
        }
    }

    fun setModEnabled(mod: Mod, enabled: Boolean) {
        val save = currentEnabledCache == null
        if (save) {
            currentEnabledCache = enabledMods.toMutableSet()
        }

        if (enabled) {
            currentEnabledCache!!.add(mod)
            currentEnabledCache!!.filter { it.name in mod.incompatibleWith || it.incompatibleWith.contains(mod.name) }
                .forEach { setModEnabled(it, false) }
            mainProfileConfig.disabledMods(currentEnabledCache!!).filter { it.name in mod.requires }
                .forEach { setModEnabled(it, true) }
        } else {
            currentEnabledCache!!.remove(mod)
            // if a mod is disabled, disable all mods that depend on it
            currentEnabledCache!!.filter { it.requires.contains(mod.name) }
                .forEach { setModEnabled(it, false) }

            // if a mod is disabled, and the dependency is only used by this mod, disable the dependency too, unless it's not marked as a dependency
            currentEnabledCache!!.filter { dep ->
                mod.requires.contains(dep.name)  // if the mod depends on this dependency
                        && dep.dependency // if the dependency is marked as a dependency
                        && currentEnabledCache!!.none { it.requires.contains(dep.name) }  // and no other mod depends on this dependency
                        && !mainProfile.modGroups.filterValues { it.contains(dep) }.keys.any { it.forceEnabled } // and the group the dependency is in is not force enabled
            }.forEach { setModEnabled(it, false) }
        }

        if (save) {
            mainProfileConfig = mainProfileConfig.copy(
                fullEnabledGroups = mainProfile.modGroups
                    .filter { currentEnabledCache!!.containsAll(it.value) }.keys
                    .map { it.name }.toSet(),
                toggledMods = currentEnabledCache!!.mapTo(mutableSetOf()) { it.name },
                seenGroups = mainProfile.groups.map { it.name }.toSet(),
                updateId = mainProfileConfig.updateId + 1,
            )
            currentEnabledCache = null
            profileConfigs = profileConfigs.plus(mainProfile.instanceId to mainProfileConfig)
            save()
        }
    }

    suspend fun update(profile: Profile) = coroutineScope {
        val config = profileConfigs[profile.instanceId]!!
        val profileCreated = minecraftValid &&
                FabricInstaller.isProfileInstalled(Dirs.minecraft, profile.instanceId) &&
                PrismInstaller.isProfileInstalled(profile)
        val fabricUpToDate = profileCreated && config.isUpToDate
        val profileUpToDate = config.installedVersion == profile.getVersionId()

        // If the profile or fabric is out of date, update that!
        if (!profileUpToDate || !fabricUpToDate)
            installFabric(profile)

        // Update the server list
        updateServers(profile)

        // Clear failed downloads before we start
        failedDownloads = emptySet()

        // Start any queued downloads
        for (mod in config.queuedDownloads) {
            launch(Dispatchers.IO) {
                download(profile, mod)
            }
        }
        for (mod in config.queuedDeletions) {
            launch(Dispatchers.IO) {
                try {
                    println("Starting deletion of $mod")
                    val file = Dirs.launchyInstances / profile.instanceId / "mods/${mod}.jar"
                    file.deleteIfExists()
                    editMutex.withLock {
                        profileConfigs = profileConfigs.plus(
                            profile.instanceId to
                                    (profileConfigs[profile.instanceId] ?: return@withLock).let { config ->
                                        config.copy(
                                            downloads = config.downloads.filter { it.key != mod },
                                            installed = config.installed.minus(mod),
                                            updateId = config.updateId + 1,
                                        )
                                    })
                    }
                    println("Successfully deleted $mod")
                } catch (e: FileSystemException) {
                    // Ignore file system exceptions
                }
            }
        }
    }

    fun updateServers(profile: Profile) {
        val newConfig = profileConfigs[profile.instanceId]!!
        newConfig.instanceFolder.createDirectories()
        val serverFile = newConfig.instanceFolder / "servers.dat"
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
    fun installFabric(profile: Profile) {
        val versionId = profile.getVersionId()
        val oldConfig = profileConfigs[profile.instanceId]!!
        val newConfig = oldConfig.copy(
            installedMinecraftVersion = profile.minecraftVersion,
            installedFabricVersion = profile.fabricVersion,
            installedVersion = versionId,
            updateId = oldConfig.updateId + 1,
        )

        FabricInstaller.installToLauncher(
            Dirs.minecraft,
            newConfig.instanceFolder,
            profile.instanceId,
            profile.displayName,
            profile.minecraftVersion,
            profile.fabricVersion,
            versionId,
        )
        PrismInstaller.installToLauncher(profile, versionId, newConfig.instanceFolder)

        if (oldConfig.installedMinecraftVersion != newConfig.installedMinecraftVersion) {
            // Move all mods into a separate folder so the user does not
            // get confused when a mod for an older version stops working
            val instanceFolder = Dirs.launchyInstances / profile.instanceId
            val intendedMods = profile.nameToMod.values.map { newConfig.getFile(it) }
            val mods = instanceFolder.resolve("mods")
            mods.takeIf { it.exists() }?.walk()?.forEach { file ->
                // Ignore directories
                if (file.isDirectory()) return@forEach

                // We can ignore mods that we intended to have!
                if (file in intendedMods) return@forEach

                // Create the target folder
                val previousMods = instanceFolder.resolve("previous-mods")
                if (!previousMods.exists()) {
                    previousMods.createDirectories()
                }

                // Copy the mod across to the previous mods folder
                file.copyTo(previousMods.resolve(mods.relativize(file)), overwrite = true)
                file.deleteIfExists()
            }
        }

        println("Finished installing profile")

        profileConfigs = profileConfigs.plus(profile.instanceId to newConfig)
        save()
        println("Finished saving profile")
    }

    suspend fun download(profile: Profile, mod: Mod) {
        val config = profileConfigs[profile.instanceId] ?: return
        runCatching {
            if (mod !in config.enabledMods.filter { config.isDownloaded(it) && config.downloads[it.name] == it.url }) {
                try {
                    println("Starting download of ${mod.name}")
                    downloading[mod] = Progress(0, 0, 0) // set progress to 0
                    Downloader.download(url = mod.url, writeTo = config.getFile(mod)) progress@{
                        downloading[mod] = it
                    }
                    editMutex.withLock {
                        profileConfigs = profileConfigs.plus(
                            profile.instanceId to
                                    (profileConfigs[profile.instanceId] ?: return@withLock).let { config ->
                                        config.copy(
                                            downloads = config.downloads.plus(mod.name to mod.url),
                                            installed = config.installed.plus(mod.name),
                                            updateId = config.updateId + 1,
                                        )
                                    })
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

            if (mod.configUrl.isNotBlank() && mod !in config.enabledMods.filter { config.configs[it.name] == it.configUrl }) {
                try {
                    println("Starting download of ${mod.name} config")
                    downloadingConfigs[mod] = Progress(0, -1, 0) // set progress to 0
                    val modConfig = config.getConfig(mod)
                    Downloader.download(url = mod.configUrl, writeTo = modConfig) progress@{
                        downloadingConfigs[mod] = it
                    }
                    editMutex.withLock {
                        profileConfigs = profileConfigs.plus(
                            profile.instanceId to
                                    (profileConfigs[profile.instanceId] ?: return@withLock).let { config ->
                                        config.copy(
                                            configs = config.configs.plus(mod.name to mod.configUrl),
                                            updateId = config.updateId + 1,
                                        )
                                    })
                    }
                    unzip(modConfig.toFile(), config.instanceFolder.toString())
                    modConfig.toFile().delete()
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
        }
    }

    suspend fun addProfile(url: String) {
        // Try to download the new file
        try {
            val (profiles, error) = Profile.readAll(listOf(url))
            if (error.isNotBlank()) {
                errorMessage = "The given URL does not contain a valid profile!"
                return
            }

            // Add the profiles to the list
            allProfilesByUrl = allProfilesByUrl + profiles
            profileConfigs = profileConfigs + profiles.entries.filter { it.value.instanceId !in profileConfigs }
                .associate { it.value.instanceId to ProfileConfig(it.value, it.value.instanceId, it.key) }

            // Change to this profile
            changeProfile(url)

            // Switch to the profile screen immediately to show it
            initialProfileDialog = false
            openScreen(Screen.Profiles)
        } catch (x: Throwable) {
            x.printStackTrace()
            errorMessage = "The given URL does not contain a valid profile!"
        }
    }

    fun changeProfile(url: String) {
        mainProfileUrl = url
        mainProfile = allProfilesByUrl[url]!!
        mainProfileConfig = profileConfigs[mainProfile.instanceId]!!
        save()
    }

    fun removeProfile(profile: Profile) {
        val config = profileConfigs[profile.instanceId] ?: return
        allProfilesByUrl = allProfilesByUrl.minus(config.profileUrl)
        profileConfigs = profileConfigs.minus(profile.instanceId)
        save()
    }

    fun save() {
        config.copy(
            handledFirstLaunch = handledFirstLaunch,
            profiles = profileConfigs,
            mainProfile = mainProfileUrl,
            launchyVersion = Config.LAUNCHY_VERSION,
        ).save()
    }
}
