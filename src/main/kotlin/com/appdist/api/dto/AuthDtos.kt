package com.appdist.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpRequest(val email: String)

@Serializable
data class VerifyOtpRequest(val email: String, val otp: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class AuthResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class MessageResponse(val message: String)
