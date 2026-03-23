package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class Project(
    val id: UUID,
    val workspaceId: UUID,
    val name: String,
    val packageName: String,
    val iconUrl: String?,
    val createdAt: Instant,
)
