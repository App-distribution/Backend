package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

enum class UserRole { ADMIN, UPLOADER, TESTER, VIEWER }

data class User(
    val id: UUID,
    val workspaceId: UUID?,   // nullable: set after workspace creation in same tx
    val email: String,
    val name: String,
    val role: UserRole,
    val fcmToken: String?,
    val createdAt: Instant,
)
