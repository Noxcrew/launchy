package com.noxcrew.launchy.data

import com.noxcrew.launchy.util.OS
import kotlin.io.path.*

object Dirs {
    val home = Path(System.getProperty("user.home"))
    
    val minecraft = when (OS.get()) {
        OS.WINDOWS -> Path(System.getenv("APPDATA")) / ".minecraft"
        OS.MAC -> home / "Library/Application Support/minecraft"
        OS.LINUX -> home / ".minecraft"
    }

    val mcclaunchy = when (OS.get()) {
        OS.WINDOWS -> Path(System.getenv("APPDATA")) / ".mcclaunchy"
        OS.MAC -> home / "Library/Application Support/mcclaunchy"
        OS.LINUX -> home / ".mcclaunchy"
    }

    val prism = when (OS.get()) {
        OS.WINDOWS -> Path(System.getenv("APPDATA")) / "PrismLauncher"
        OS.MAC -> home / "Library/Application Support/PrismLauncher"
        OS.LINUX -> home / ".local/share/PrismLauncher"
    }

    val mods = mcclaunchy / "mods"
    val previousMods = mcclaunchy / "previous-mods"
    val tmp = mcclaunchy / ".tmp"

    val config = when (OS.get()) {
        OS.WINDOWS -> Path(System.getenv("APPDATA"))
        OS.MAC -> home / "Library/Application Support"
        OS.LINUX -> home / ".config"
    } / ".mcclaunchy"

    val configFile = config / "mcclaunchy-launcher.yml"
    val versionsFolder = config / "versions"

    @OptIn(ExperimentalPathApi::class)
    fun createDirs() {
        config.createDirectories()
        mcclaunchy.createDirectories()
        mods.createDirectories()
        tmp.deleteRecursively()
        tmp.createDirectories()
    }
}
