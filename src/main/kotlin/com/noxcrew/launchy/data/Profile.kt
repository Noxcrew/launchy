package com.noxcrew.launchy.data

import com.noxcrew.launchy.logic.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream

@Serializable
data class Profile(
    val groups: Set<Group> = emptySet(),
    @SerialName("modGroups")
    private val _modGroups: Map<GroupName, Set<Mod>> = emptyMap(),
    val servers: Set<Server> = emptySet(),
    val fabricVersion: String = "",
    val minecraftVersion: String = "",
    val prefix: String? = null,
    val id: String? = null,
    val version: String? = null,
    val valid: Boolean = true,
    val linked: List<String> = emptyList(),
    val info: ProfileInfo? = null,
) {
    val nameToGroup: Map<GroupName, Group> = groups.associateBy { it.name }

    @Transient
    val modGroups: Map<Group, Set<Mod>> = _modGroups.mapNotNull { nameToGroup[it.key]?.to(it.value) }.toMap()
    val nameToMod: Map<ModName, Mod> = modGroups.values
        .flatten()
        .associateBy { it.name }

    fun getVersionId(configVersion: String): String = if (prefix != null && version != null) {
        String.format("%s-%s-%s.%s", prefix, id ?: "latest", configVersion, version)
    } else {
        String.format("fabric-loader-%s-%s", fabricVersion, minecraftVersion)
    }

    companion object {
        suspend fun readAll(urls: List<String>): Pair<Map<String, Profile>, String> {
            var errorMessage = ""
            val profiles = mutableMapOf<String, Profile>()
            val attempted = mutableSetOf<String>()
            suspend fun tryRead(url: String) {
                if (!attempted.add(url)) return
                val profile = try {
                    Dirs.versionsFolder.createDirectories()
                    read(url, Dirs.versionsFolder.resolve(url.substringAfterLast("/")))
                } catch (x: Throwable) {
                    x.printStackTrace()
                    errorMessage = "An error occurred while loading version information. Please contact an administrator for assistance!"
                    Profile(valid = false)
                }
                profiles[url] = profile
                profile.linked.forEach { tryRead(it) }
            }
            urls.forEach { tryRead(it) }
            return profiles to errorMessage
        }

        suspend fun read(url: String, target: Path): Profile = withContext(Dispatchers.IO) {
            println("Fetching profile from $url to ${target.absolutePathString()}")
            Downloader.download(url, target)
            Formats.yaml.decodeFromStream(serializer(), target.inputStream())
        }
    }
}
