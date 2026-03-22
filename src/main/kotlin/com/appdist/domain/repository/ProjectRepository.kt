package com.appdist.domain.repository

import com.appdist.domain.model.Project
import java.util.UUID

interface ProjectRepository {
    suspend fun create(workspaceId: UUID, name: String, packageName: String): Project
    suspend fun findById(id: UUID): Project?
    suspend fun listByWorkspace(workspaceId: UUID): List<Project>
    suspend fun delete(id: UUID)
}
