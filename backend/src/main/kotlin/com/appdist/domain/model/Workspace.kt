package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class Workspace(
    val id: UUID,
    val name: String,
    val slug: String,
    val ownerId: UUID?,   // nullable: user created after workspace, then ownerId set
    val createdAt: Instant,
)
