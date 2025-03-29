package com.noxcrew.launchy.data

import kotlinx.serialization.Serializable

@Serializable
data class Mod(
    val name: String,
    val displayName: String = "",
    val license: String = "Unknown",
    val homepage: String = "",
    val desc: String,
    val url: String,
    val configUrl: String = "",
    val configDesc: String = "",
    val forceConfigDownload: Boolean = false,
    val dependency: Boolean = false,
    val incompatibleWith: List<String> = emptyList(),
    val requires: List<String> = emptyList(),
) {

    // Compare only using the name!
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mod) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
