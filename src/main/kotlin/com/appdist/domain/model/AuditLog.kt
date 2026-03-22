package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class AuditLog(
    val id: UUID,
    val userId: UUID,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val metadata: Map<String, String>?,
    val createdAt: Instant,
)
