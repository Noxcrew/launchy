package com.noxcrew.launchy.logic

import com.noxcrew.launchy.util.OS
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists

sealed class Installation(val type: Type) {
    enum class Type {
        PRISM,
        MINECRAFT,
    }

    abstract fun start()

    class Executable(type: Type, val executable: Path) : Installation(type) {
        override fun start() {
            ProcessBuilder(executable.toString()).start()
        }
    }

    class UwpPackage(type: Type, val packageFamily: String): Installation(type) {
        init {
            require(OS.get() == OS.WINDOWS)
        }

        override fun start() {
            val command = listOf("powershell.exe", "start", "shell:AppsFolder\\${packageFamily}!Minecraft")
            ProcessBuilder(command).start()
        }
    }

    class MacBundle(type: Type, val bundle: Path): Installation(type) {
        init {
            require(OS.get() == OS.MAC)
        }

        override fun start() {
            ProcessBuilder(listOf("open", bundle.toString())).start()
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
                    result += Installation.UwpPackage(Installation.Type.MINECRAFT, it)
                }

                findMinecraftWindowsExe()?.also {
                    result += Installation.Executable(Installation.Type.MINECRAFT, Path(it))
                }
            }

            OS.LINUX -> {
                findCommand("minecraft-launcher")?.also {
                    result += Installation.Executable(Installation.Type.MINECRAFT, it)
                }
            }

            OS.MAC -> {
                findMacBundle("Minecraft")?.also {
                    result += Installation.MacBundle(Installation.Type.MINECRAFT, it)
                }
            }
        }

        return result
    }

    private fun findPrism(): Installation? = when (OS.get()) {
            OS.WINDOWS -> findExecutable(
                listOf(
                    Path(System.getenv("ProgramFiles")) / "PrismLauncher/PrismLauncher.exe",
                    Path(System.getenv("ProgramFiles(x86)")) / "PrismLauncher/PrismLauncher.exe",
                    Path(System.getenv("APPDATA")) / "PrismLauncher.exe",
                    Path(System.getenv("LOCALAPPDATA")) / "Programs/PrismLauncher/PrismLauncher.exe"
                )
            )?.let { Installation.Executable(Installation.Type.PRISM, it) }

            OS.LINUX -> findCommand("prismlauncher")?.let { Installation.Executable(Installation.Type.PRISM, it) }

            OS.MAC -> findMacBundle("Prism Launcher")?.let { Installation.MacBundle(Installation.Type.PRISM, it) }
        }


    private fun findMacBundle(name: String): Path? = listOf(
        "/Applications/$name.app",
        "/Users/${System.getProperty("user.name")}/Applications/$name.app"
    )
        .firstNotNullOfOrNull { Path(it).takeIf(Path::exists) }

    private fun findCommand(command: String): Path? {
        return try {
            val process = ProcessBuilder("which", command)
                .redirectErrorStream(true)
                .start()

            val result = process.inputStream
                .bufferedReader()
                .readText()
                .trim()

            if (process.waitFor() == 0 && result.isNotBlank()) {
                Path(result)
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