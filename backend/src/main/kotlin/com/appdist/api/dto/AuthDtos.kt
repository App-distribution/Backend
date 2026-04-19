package com.appdist.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class CreateUserRequest(val email: String, val name: String, val role: String)

@Serializable
data class CreateUserResponse(
    val user: UserDto,
    @SerialName("generated_password") val generatedPassword: String,
)

@Serializable
data class ResetPasswordResponse(
    @SerialName("generated_password") val generatedPassword: String,
)
