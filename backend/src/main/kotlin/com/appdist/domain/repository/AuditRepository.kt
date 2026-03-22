package com.appdist.domain.repository

import com.appdist.domain.model.AuditLog
import java.util.UUID

interface AuditRepository {
    suspend fun log(
        userId: UUID,
        action: String,
        resourceType: String,
        resourceId: UUID? = null,
        metadata: Map<String, String>? = null,
    )
    suspend fun list(resourceType: String? = null, page: Int = 0, limit: Int = 50): List<AuditLog>
}
