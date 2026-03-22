package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectResponse(
    @SerialName("id") val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("name") val name: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("created_at") val createdAt: Long
)
