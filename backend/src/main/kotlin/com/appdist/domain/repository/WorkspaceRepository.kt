package com.appdist.domain.repository

import com.appdist.domain.model.Workspace
import java.util.UUID

interface WorkspaceRepository {
    suspend fun create(name: String, slug: String): Workspace   // no ownerId — set separately
    suspend fun findById(id: UUID): Workspace?
    suspend fun findBySlug(slug: String): Workspace?
    suspend fun updateOwnerId(id: UUID, ownerId: UUID)           // set after user creation
    suspend fun listAll(): List<Workspace>
}
