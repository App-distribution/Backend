package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String,
    @SerialName("workspace_id") val workspaceId: String
)

@Serializable
data class UpdateMeRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null
)
