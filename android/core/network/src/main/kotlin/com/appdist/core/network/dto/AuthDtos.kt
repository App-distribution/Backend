package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpRequest(@SerialName("email") val email: String)

@Serializable
data class VerifyOtpRequest(
    @SerialName("email") val email: String,
    @SerialName("otp") val otp: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)
