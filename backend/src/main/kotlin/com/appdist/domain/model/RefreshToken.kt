package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expiresAt: Instant,
    val revoked: Boolean,
)
