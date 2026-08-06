package com.appdist.domain.repository

import com.appdist.domain.model.ApiKey
import java.util.UUID

interface ApiKeyRepository {
    suspend fun create(apiKey: ApiKey): ApiKey
    suspend fun findActiveByHash(keyHash: String): ApiKey?
    suspend fun listByWorkspace(workspaceId: UUID): List<ApiKey>
    /** Возвращает true, если ключ найден в этом workspace и отозван. */
    suspend fun revoke(id: UUID, workspaceId: UUID): Boolean
    suspend fun touchLastUsed(id: UUID)
}
