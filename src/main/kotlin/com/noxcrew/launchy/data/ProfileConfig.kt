package com.noxcrew.launchy.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists

@Serializable
data class ProfileConfig(
    @Transient
    private val _profile: Profile? = null,
    val instanceId: String = "mc-championship",
    val profileUrl: String,
    val fullEnabledGroups: Set<GroupName> = setOf(),
    val fullDisabledGroups: Set<GroupName> = setOf(),
    val toggledMods: Set<ModName> = setOf(),
    val downloads: Map<ModName, DownloadURL> = mapOf(),
    val configs: Map<ModName, ConfigURL> = mapOf(),
    val seenGroups: Set<GroupName> = setOf(),
    val installed: Set<ModName> = setOf(),
    val installedFabricVersion: String? = null,
    val installedMinecraftVersion: String? = null,
    val installedVersion: String? = null,
    val updateId: Int = 0
) {
    val profile: Profile
        get() {
            val profile =  requireNotNull(_profile) { "Profile was not present" }
            require (profile.instanceId == instanceId) { "Profile did not match own instance id" }
            return profile
        }

    val downloadUrls: Map<Mod, DownloadURL> by lazy {
        downloads
            .mapNotNull { profile.getMod(it.key)?.to(it.value) }
            .toMap()
    }

    val downloadConfigUrls: Map<Mod, DownloadURL> by lazy {
        configs
            .mapNotNull { profile.getMod(it.key)?.to(it.value) }
            .toMap()
    }

    val isUpToDate: Boolean
        get() = (installedMinecraftVersion == profile.minecraftVersion &&
                installedFabricVersion == profile.fabricVersion)

    val downloadedMods: List<Mod> by lazy {
        profile.nameToMod.values.filter { isDownloaded(it) }
    }

    val enabledMods: Set<Mod>
        get() {
        val defaultEnabled = profile.groups
            .filter { it.enabledByDefault }
            .map { it.name } - seenGroups
        val fullEnabled = fullEnabledGroups
        val forceEnabled = profile.groups.filter { it.forceEnabled }.map { it.name }
        val forceDisabled = profile.groups.filter { it.forceDisabled }
        val fullDisabled = fullDisabledGroups

        val enabled = toggledMods.mapNotNull { profile.getMod(it) }.toMutableSet()
        enabled.addAll(
            ((fullEnabled + defaultEnabled + forceEnabled).toSet())
                .mapNotNull { profile.getGroup(it) }
                .mapNotNull { profile.modGroups[it] }.flatten()
        )
        enabled.removeAll((forceDisabled + fullDisabled).toSet().mapNotNull { profile.modGroups[it] }.flatten().toSet())
        return enabled
    }

    val queuedDownloads: Set<Mod>
        get() {
            val enabledModsWithConfig = enabledMods.filter { it.configUrl != "" }
            val upToDateMods = enabledMods.filter { it in downloadedMods && downloadUrls[it] == it.url }
            val upToDateConfigs = enabledMods.filter { downloadConfigUrls[it] == it.configUrl }
            return (enabledMods - upToDateMods.toSet()) + (enabledModsWithConfig - upToDateConfigs.toSet())
        }
    val queuedUpdates : Set<Mod>
        get() = queuedDownloads.filter { it in downloadedMods }.toSet()

    val queuedInstalls: Set<Mod>
        get() = queuedDownloads - queuedUpdates

    val queuedDeletions: List<ModName>
        get() = disabledMods(enabledMods).filter { it in downloadedMods }.map { it.name } +
                (installed - profile.nameToMod.keys.toSet())

    fun disabledMods(enabledMods: Set<Mod>): Set<Mod> =
        profile.nameToMod.values.toSet() - enabledMods

    val instanceFolder: Path
        get() = Dirs.launchyInstances / instanceId

    fun getFile(mod: Mod) = instanceFolder / "mods/${mod.name}.jar"
    fun getConfig(mod: Mod) = instanceFolder / "tmp/${mod.name}-config.zip"
    fun isDownloaded(mod: Mod) = getFile(mod).exists()
}