package com.noxcrew.launchy.data

import kotlinx.serialization.Serializable

@Serializable
data class ProfileInfo(
    val name: String,
    val description: String?,
    val icon: String?,
)
