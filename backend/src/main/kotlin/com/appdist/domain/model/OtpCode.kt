package com.appdist.domain.model

import kotlinx.datetime.Instant

data class OtpCode(
    val email: String,
    val code: String,
    val expiresAt: Instant,
    val used: Boolean,
)
