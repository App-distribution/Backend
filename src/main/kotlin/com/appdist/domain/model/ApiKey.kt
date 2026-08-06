package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class ApiKey(
    val id: UUID,
    val workspaceId: UUID,
    val createdBy: UUID,
    val name: String,
    val keyHash: String,
    val prefix: String,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
    val revoked: Boolean,
)
