package com.appdist.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(val name: String, val packageName: String)

@Serializable
data class ProjectDto(
    val id: String,
    val workspaceId: String,
    val name: String,
    val packageName: String,
    val iconUrl: String?,
    val createdAt: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val workspaceId: String?,
)

@Serializable
data class UpdateProfileRequest(val name: String? = null, val fcmToken: String? = null)
