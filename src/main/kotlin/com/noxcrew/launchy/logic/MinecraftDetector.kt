package com.noxcrew.launchy.logic

import com.noxcrew.launchy.util.OS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists

sealed class Installation {
    abstract fun start()

    data class Prism(val executable: Path) : Installation() {
        override fun start() {
            ProcessBuilder(executable.toString()).start()
        }
    }

    data class Minecraft(val executable: Path) : Installation() {
        override fun start() {
            ProcessBuilder(executable.toString()).start()
        }
    }

    data class MinecraftUwp(val packageFamily: String) : Installation() {
        override fun start() {
            val command = listOf("powershell.exe", "start", "shell:AppsFolder\\${packageFamily}!Minecraft")
            ProcessBuilder(command).start()
        }
    }
}

object MinecraftDetector {

    fun detectInstallations(): List<Installation> {
        val result = mutableListOf<Installation>()

        findPrism()?.also { result += it }

        when (OS.get()) {
            OS.WINDOWS -> {
                findMinecraftLauncherUwpPackage()?.also {
                    result += Installation.MinecraftUwp(it)
                }

                findMinecraftWindowsExe()?.also {
                    result += Installation.Minecraft(Path(it))
                }
            }

            OS.LINUX -> {
                findCommand("minecraft-launcher")?.also {
                    result += Installation.Minecraft(Path(it))
                }
            }

            OS.MAC -> {
                findMinecraftMac()?.also {
                    result += Installation.Minecraft(Path(it))
                }
            }
        }

        return result
    }

    private fun findPrism(): Installation.Prism? {
        val executable = when (OS.get()) {
            OS.WINDOWS -> findExecutable(
                listOf(
                    Path(System.getenv("ProgramFiles")) / "PrismLauncher/PrismLauncher.exe",
                    Path(System.getenv("ProgramFiles(x86)")) / "PrismLauncher/PrismLauncher.exe",
                    Path(System.getenv("APPDATA")) / "PrismLauncher.exe",
                    Path(System.getenv("LOCALAPPDATA")) / "Programs/PrismLauncher/PrismLauncher.exe"
                )
            )

            OS.LINUX -> findCommand("prismlauncher")?.let { Path(it) }

            OS.MAC -> findPrismMac()?.let { Path(it) }
        } ?: return null
        return Installation.Prism(executable)
    }

    private fun findMinecraftMac(): String? {
        val path = "/Applications/Minecraft.app/Contents/MacOS/launcher"
        return path.takeIf { File(it).canExecute() }
    }

    private fun findPrismMac(): String? {
        val path = "/Applications/PrismLauncher.app/Contents/MacOS/PrismLauncher"
        return path.takeIf { File(it).canExecute() }
    }

    private fun findCommand(command: String): String? {
        return try {
            val process = ProcessBuilder("which", command)
                .redirectErrorStream(true)
                .start()

            val result = process.inputStream
                .bufferedReader()
                .readText()
                .trim()

            if (process.waitFor() == 0 && result.isNotBlank()) {
                result
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findMinecraftWindowsExe(): String? {
        val candidates = listOfNotNull(
            System.getenv("ProgramFiles")?.let {
                Path(it) / "Minecraft Launcher/MinecraftLauncher.exe"
            },
            System.getenv("ProgramFiles(x86)")?.let {
                Path(it) / "Minecraft Launcher/MinecraftLauncher.exe"
            },
            System.getenv("LOCALAPPDATA")?.let {
                Path(it) / "Programs/Minecraft Launcher/MinecraftLauncher.exe"
            }
        )

        return candidates.firstOrNull { it.exists() }?.toString()
    }

    private fun findMinecraftLauncherUwpPackage(): String? {
        val process = ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-Command",
            """
                Get-AppxPackage Microsoft.4297127D64EC6 |
                Select -ExpandProperty PackageFamilyName
            """.trimIndent()
        ).start()

        return process.inputStream
            .bufferedReader()
            .readLines()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
    }

    private fun findExecutable(paths: List<Path>): Path? =
        paths.firstOrNull { it.exists() }
}