package com.appdist.domain.repository

import com.appdist.domain.model.RefreshToken
import java.util.UUID

interface RefreshTokenRepository {
    suspend fun create(userId: UUID, token: String, ttlDays: Long): RefreshToken
    suspend fun findValid(token: String): RefreshToken?
    suspend fun revoke(token: String)
    suspend fun revokeAllForUser(userId: UUID)
}
