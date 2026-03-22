package com.appdist.domain.repository

import com.appdist.domain.model.Build
import com.appdist.domain.model.BuildStatus
import com.appdist.domain.model.ReleaseChannel
import java.util.UUID

interface BuildRepository {
    suspend fun create(build: Build): Build
    suspend fun findById(id: UUID): Build?
    suspend fun listByProject(
        projectId: UUID,
        channel: ReleaseChannel? = null,
        search: String? = null,
        page: Int = 0,
        limit: Int = 20,
    ): List<Build>
    suspend fun listRecent(workspaceId: UUID, limit: Int = 20): List<Build>
    suspend fun update(buildId: UUID, changelog: String?, status: BuildStatus): Build
    suspend fun delete(buildId: UUID)
    suspend fun findByChecksum(checksum: String): Build?
    suspend fun unsetLatestInChannel(projectId: UUID, channel: ReleaseChannel)
}
