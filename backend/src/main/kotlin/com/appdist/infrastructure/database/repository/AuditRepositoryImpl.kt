package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.AuditLog
import com.appdist.domain.repository.AuditRepository
import com.appdist.infrastructure.database.tables.AuditLogsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class AuditRepositoryImpl : AuditRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun log(
        userId: UUID,
        action: String,
        resourceType: String,
        resourceId: UUID?,
        metadata: Map<String, String>?,
    ) = dbQuery {
        AuditLogsTable.insert {
            it[AuditLogsTable.id] = UUID.randomUUID()
            it[AuditLogsTable.userId] = userId
            it[AuditLogsTable.action] = action
            it[AuditLogsTable.resourceType] = resourceType
            it[AuditLogsTable.resourceId] = resourceId
            it[AuditLogsTable.metadata] = metadata?.let { m -> Json.encodeToString(m) }
            it[AuditLogsTable.createdAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun list(resourceType: String?, page: Int, limit: Int): List<AuditLog> = dbQuery {
        AuditLogsTable.selectAll()
            .apply { if (resourceType != null) where { AuditLogsTable.resourceType eq resourceType } }
            .orderBy(AuditLogsTable.createdAt, SortOrder.DESC)
            .limit(limit).offset(page.toLong() * limit)
            .map {
                AuditLog(
                    id = it[AuditLogsTable.id],
                    userId = it[AuditLogsTable.userId],
                    action = it[AuditLogsTable.action],
                    resourceType = it[AuditLogsTable.resourceType],
                    resourceId = it[AuditLogsTable.resourceId],
                    metadata = it[AuditLogsTable.metadata]?.let { json ->
                        runCatching { Json.decodeFromString<Map<String, String>>(json) }.getOrNull()
                    },
                    createdAt = it[AuditLogsTable.createdAt],
                )
            }
    }
}
