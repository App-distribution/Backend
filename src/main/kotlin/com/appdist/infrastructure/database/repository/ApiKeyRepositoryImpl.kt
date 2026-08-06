package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.ApiKey
import com.appdist.domain.repository.ApiKeyRepository
import com.appdist.infrastructure.database.tables.ApiKeysTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ApiKeyRepositoryImpl : ApiKeyRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun toModel(row: ResultRow) = ApiKey(
        id = row[ApiKeysTable.id],
        workspaceId = row[ApiKeysTable.workspaceId],
        createdBy = row[ApiKeysTable.createdBy],
        name = row[ApiKeysTable.name],
        keyHash = row[ApiKeysTable.keyHash],
        prefix = row[ApiKeysTable.prefix],
        createdAt = row[ApiKeysTable.createdAt],
        lastUsedAt = row[ApiKeysTable.lastUsedAt],
        revoked = row[ApiKeysTable.revoked],
    )

    override suspend fun create(apiKey: ApiKey): ApiKey {
        dbQuery {
            ApiKeysTable.insert {
                it[id] = apiKey.id
                it[workspaceId] = apiKey.workspaceId
                it[createdBy] = apiKey.createdBy
                it[name] = apiKey.name
                it[keyHash] = apiKey.keyHash
                it[prefix] = apiKey.prefix
                it[createdAt] = apiKey.createdAt
                it[lastUsedAt] = apiKey.lastUsedAt
                it[revoked] = apiKey.revoked
            }
        }
        return apiKey
    }

    override suspend fun findActiveByHash(keyHash: String): ApiKey? = dbQuery {
        ApiKeysTable.selectAll()
            .where { (ApiKeysTable.keyHash eq keyHash) and (ApiKeysTable.revoked eq false) }
            .singleOrNull()
            ?.let(::toModel)
    }

    override suspend fun listByWorkspace(workspaceId: UUID): List<ApiKey> = dbQuery {
        ApiKeysTable.selectAll()
            .where { ApiKeysTable.workspaceId eq workspaceId }
            .orderBy(ApiKeysTable.createdAt to SortOrder.DESC)
            .map(::toModel)
    }

    // workspaceId в условии не для удобства, а для изоляции: без него админ
    // одного пространства мог бы отозвать чужой ключ, зная его id.
    override suspend fun revoke(id: UUID, workspaceId: UUID): Boolean = dbQuery {
        ApiKeysTable.update({
            (ApiKeysTable.id eq id) and (ApiKeysTable.workspaceId eq workspaceId)
        }) {
            it[revoked] = true
        } > 0
    }

    override suspend fun touchLastUsed(id: UUID) = dbQuery {
        ApiKeysTable.update({ ApiKeysTable.id eq id }) {
            it[lastUsedAt] = Clock.System.now()
        }
        Unit
    }
}
