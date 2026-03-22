package com.appdist.domain.repository

import com.appdist.domain.model.User
import com.appdist.domain.model.UserRole
import java.util.UUID

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findAllByWorkspace(workspaceId: UUID): List<User>   // for FCM targeting
    suspend fun create(workspaceId: UUID, email: String, name: String, role: UserRole): User
    suspend fun updateFcmToken(userId: UUID, token: String?)
    suspend fun update(userId: UUID, name: String): User
}
