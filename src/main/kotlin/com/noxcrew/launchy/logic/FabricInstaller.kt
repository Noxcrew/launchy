/*
 * File licensed under GNU Lesser General Public License v3.0
 *
 * Original can be found at:
 * https://github.com/IrisShaders/Iris-Installer/blob/master/src/main/java/net/hypercubemc/iris_installer/VanillaLauncherIntegration.java
 */
package com.noxcrew.launchy.logic

import mjson.Json
import net.fabricmc.installer.client.ProfileInstaller
import net.fabricmc.installer.client.ProfileInstaller.LauncherType
import net.fabricmc.installer.util.FabricService
import net.fabricmc.installer.util.Utils
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays
import java.util.Base64
import java.util.Date
import java.util.stream.Collectors
import javax.swing.JOptionPane
import kotlin.io.path.exists

object FabricInstaller {
    fun isProfileInstalled(mcDir: Path, name: String): Boolean {
        val launcherProfiles: Path = mcDir.resolve("launcher_profiles.json")
        if (!launcherProfiles.exists()) return false
        val jsonObject = JSONObject(Utils.readString(launcherProfiles))
        val profiles: JSONObject = jsonObject.getJSONObject("profiles")
        return profiles.has(name)
    }

    fun installToLauncher(
        vanillaGameDir: Path,
        instanceDir: Path,
        profileName: String,
        gameVersion: String,
        loaderVersion: String,
        versionId: String,
    ): Boolean {
        installVersion(vanillaGameDir, gameVersion, loaderVersion, versionId)
        installProfile(vanillaGameDir, instanceDir, profileName, versionId)
        return true
    }

    fun bumpProfile(
        mcDir: Path,
        profileName: String,
    ) {
        val launcherProfiles: Path = mcDir.resolve("launcher_profiles.json")
        if (!Files.exists(launcherProfiles)) {
            println("Could not find launcher_profiles")
            return
        }
        println("Bumping profile")
        val jsonObject = JSONObject(Utils.readString(launcherProfiles))
        val profiles: JSONObject = jsonObject.getJSONObject("profiles")
        if (!profiles.has(profileName)) {
            println("Could not find profile in list")
            return
        }
        val profile: JSONObject = profiles.getJSONObject(profileName)
        profile.put("lastUsed", Utils.ISO_8601.format(Date())) // Update timestamp to bring to top of profile list
        profiles.put(profileName, profile)
        jsonObject.put("profiles", profiles)
        Utils.writeToFile(launcherProfiles, jsonObject.toString())
    }

    fun installVersion(
        mcDir: Path,
        gameVersion: String,
        loaderVersion: String,
        versionId: String,
    ) {
        println("Installing $gameVersion with fabric $loaderVersion under $versionId")
        val versionsDir = mcDir.resolve("versions")
        val profileDir = versionsDir.resolve(versionId)
        val profileJsonPath = profileDir.resolve("$versionId.json")
        if (!Files.exists(profileDir)) {
            Files.createDirectories(profileDir)
        }
        val dummyJar = profileDir.resolve("$versionId.jar")
        Files.deleteIfExists(dummyJar)
        Files.createFile(dummyJar)
        val profileJson: Json = FabricService.queryMetaJson("v2/versions/loader/$gameVersion/$loaderVersion/profile/json")
        Utils.writeToFile(profileJsonPath, profileJson.toString().replace("fabric-loader-$loaderVersion-$gameVersion", versionId))
    }

    private fun installProfile(
        mcDir: Path,
        instanceDir: Path,
        profileName: String,
        versionId: String,
    ) {
        val launcherProfiles: Path = mcDir.resolve("launcher_profiles.json")
        if (!Files.exists(launcherProfiles)) {
            println("Could not find launcher_profiles")
            return
        }
        println("Creating profile")
        val jsonObject = JSONObject(Utils.readString(launcherProfiles))
        val profiles: JSONObject = jsonObject.getJSONObject("profiles")
        var foundProfileName: String? = profileName
        val it: Iterator<String> = profiles.keys()
        while (it.hasNext()) {
            val key = it.next()
            val foundProfile: JSONObject = profiles.getJSONObject(key)
            if (foundProfile.has("lastVersionId") && foundProfile.getString("lastVersionId")
                    .equals(versionId) && foundProfile.has("gameDir") && foundProfile.getString("gameDir")
                    .equals(instanceDir.toString())
            ) {
                foundProfileName = key
            }
        }

        // If the profile already exists, use it instead of making a new one so that user's settings are kept (e.g icon)
        val profile: JSONObject =
            if (profiles.has(foundProfileName)) profiles.getJSONObject(foundProfileName) else createProfile(
                profileName,
                instanceDir,
                versionId,
            )
        profile.put("name", profileName)
        profile.put("lastUsed", Utils.ISO_8601.format(Date())) // Update timestamp to bring to top of profile list
        profile.put("lastVersionId", versionId)
        profiles.put(foundProfileName, profile)
        jsonObject.put("profiles", profiles)
        Utils.writeToFile(launcherProfiles, jsonObject.toString())
    }

    private fun createProfile(name: String, instanceDir: Path, versionId: String): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("type", "custom")
        jsonObject.put("created", Utils.ISO_8601.format(Date()))
        jsonObject.put("gameDir", instanceDir.toString())
        jsonObject.put("lastUsed", Utils.ISO_8601.format(Date()))
        jsonObject.put("lastVersionId", versionId)
        jsonObject.put("icon", getProfileIcon())
        jsonObject.put("javaArgs", "-Xmx4G -XX:+UseZGC -XX:+ZGenerational")
        return jsonObject
    }

    private fun getProfileIcon(): String {
        return try {
            val input: InputStream = Utils::class.java.classLoader.getResourceAsStream("launcher_icon.png")!!
            val var4: String
            try {
                var ret = ByteArray(4096)
                var offset = 0
                var len: Int
                while (input.read(ret, offset, ret.size - offset).also { len = it } != -1) {
                    offset += len
                    if (offset == ret.size) {
                        ret = Arrays.copyOf(ret, ret.size * 2)
                    }
                }
                var4 = "data:image/png;base64," + Base64.getEncoder().encodeToString(ret.copyOf(offset))
            } catch (e: Throwable) {
                try {
                    input.close()
                } catch (var5: Throwable) {
                    e.addSuppressed(var5)
                }
                throw e
            }
            input.close()
            var4
        } catch (e: IOException) {
            e.printStackTrace()
            "TNT"
        }
    }
}
