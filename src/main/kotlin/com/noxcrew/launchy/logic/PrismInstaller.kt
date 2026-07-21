package com.noxcrew.launchy.logic

import com.noxcrew.launchy.data.Dirs
import com.noxcrew.launchy.data.Profile
import com.noxcrew.launchy.util.OS
import mjson.Json
import net.fabricmc.installer.util.FabricService
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText


object PrismInstaller {
    fun isProfileInstalled(profile: Profile): Boolean {
        val prism = Dirs.prism

        // If Prism is not installed, ignore installing profiles!
        if (!prism.exists()) return true

        // Determine if the instance folder exists
        return prism.resolve("instances/${profile.instanceId}").exists()
    }

    fun installToLauncher(profile: Profile, versionId: String, realInstanceFolder: Path): Boolean {
        val prism = Dirs.prism
        if (!prism.exists()) return false
        val instanceId = profile.instanceId
        val instanceFolder = prism.resolve("instances/$instanceId")
        instanceFolder.createDirectories()

        // Download the icon
        val iconPath = prism.resolve("icons/$instanceId.png")
        profile.info?.icon?.also {
            URI.create(it).toURL().openStream().use { `in` ->
                Files.copy(`in`, iconPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        // Create the symlink
        val symLink = instanceFolder.resolve("minecraft")
        if (!symLink.exists()) {
            if (OS.get() == OS.WINDOWS) {
                val pb = ProcessBuilder(
                    "cmd", "/c",
                    "mklink", "/J",
                    symLink.toString(),
                    realInstanceFolder.toAbsolutePath().toString()
                )
                pb.inheritIO().start().waitFor()
            } else {
                Files.createSymbolicLink(symLink, realInstanceFolder.toAbsolutePath())
            }
        }

        // Create all other files
        instanceFolder.resolve("allowed_symlinks.txt").writeText(symLink.absolutePathString())
        instanceFolder.resolve("instance.cfg").writeText(
            """
            [General]
            ConfigVersion=1.3
            InstanceType=OneSix
            name=${profile.displayName} (${versionId})
            iconKey=${instanceId}
        """.trimIndent()
        )
        instanceFolder.resolve("mmc-pack.json").writeText(
            """
            {
                "components": [
                    {
                        "cachedName": "Minecraft",
                        "cachedVersion": "${profile.minecraftVersion}",
                        "important": true,
                        "uid": "net.minecraft",
                        "version": "${profile.minecraftVersion}"
                    },
                    {
                        "cachedName": "Fabric Loader",
                        "cachedVersion": "${profile.fabricVersion}",
                        "uid": "net.fabricmc.fabric-loader",
                        "version": "${profile.fabricVersion}"
                    }
                ],
                "formatVersion": 1
            }
        """.trimIndent()
        )

        return true
    }
}