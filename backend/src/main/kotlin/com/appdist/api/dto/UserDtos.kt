package com.appdist.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    @SerialName("workspace_id") val workspaceId: String?,
    @SerialName("created_at") val createdAt: String,
)
