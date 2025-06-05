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
import kotlin.io.path.inputStream

@Serializable
data class Versions(
    val groups: Set<Group> = emptySet(),
    @SerialName("modGroups")
    private val _modGroups: Map<GroupName, Set<Mod>> = emptyMap(),
    val servers: Set<Server> = emptySet(),
    val fabricVersion: String = "",
    val minecraftVersion: String = "",
    val valid: Boolean = true,
) {
    val nameToGroup: Map<GroupName, Group> = groups.associateBy { it.name }

    @Transient
    val modGroups: Map<Group, Set<Mod>> = _modGroups.mapNotNull { nameToGroup[it.key]?.to(it.value) }.toMap()
    val nameToMod: Map<ModName, Mod> = modGroups.values
        .flatten()
        .associateBy { it.name }

    companion object {
        /** If true, local configs are used for testing. */
        private const val DEBUGGING_LOCAL_CONFIGS: Boolean = false

        suspend fun readLatest(url: String, target: Path, ignoreLocal: Boolean = !DEBUGGING_LOCAL_CONFIGS): Versions = withContext(Dispatchers.IO) {
            // Check against an environment variable that no user should ever have
            if (!ignoreLocal && System.getenv()["MCC_LAUNCHY_DEV"] == "13518961351") {
                println("Reading from local versions")

                // Load from the example profile for testing locally
                val file = Path("example-profile.yml")
                Formats.yaml.decodeFromStream(serializer(), file.inputStream())
            } else {
                println("Fetching latest versions from $url to ${target.absolutePathString()}")
                Downloader.download(url, target)
                Formats.yaml.decodeFromStream(serializer(), target.inputStream())
            }
        }
    }
}
