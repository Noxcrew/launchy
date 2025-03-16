package com.noxcrew.launchy.data

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val name: String,
    val startExpanded: Boolean = false,
    val enabledByDefault: Boolean = false,
    val forceEnabled: Boolean = false,
    val forceDisabled: Boolean = false,
)
