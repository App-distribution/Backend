package com.appdist.domain.repository

import com.appdist.domain.model.User
import com.appdist.domain.model.UserRole
import java.util.UUID

data class UserWithHash(val user: User, val passwordHash: String)

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findByEmailWithHash(email: String): UserWithHash?
    suspend fun findAllByWorkspace(workspaceId: UUID): List<User>
    suspend fun create(workspaceId: UUID, email: String, name: String, role: UserRole, passwordHash: String): User
    suspend fun updatePasswordHash(userId: UUID, passwordHash: String)
    suspend fun updateFcmToken(userId: UUID, token: String?)
    suspend fun update(userId: UUID, name: String): User
}
