package com.noxcrew.launchy.data

import kotlinx.serialization.Serializable

@Serializable
data class Server(
    val name: String,
    val ip: String,
)
