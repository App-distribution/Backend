package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.Workspace
import com.appdist.domain.repository.WorkspaceRepository
import com.appdist.infrastructure.database.tables.WorkspacesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class WorkspaceRepositoryImpl : WorkspaceRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(name: String, slug: String): Workspace {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        dbQuery {
            WorkspacesTable.insert {
                it[WorkspacesTable.id] = id
                it[WorkspacesTable.name] = name
                it[WorkspacesTable.slug] = slug
                it[WorkspacesTable.ownerId] = null   // set after user creation
                it[WorkspacesTable.createdAt] = now
            }
        }
        return Workspace(id, name, slug, null, now)
    }

    override suspend fun updateOwnerId(id: UUID, ownerId: UUID) = dbQuery {
        WorkspacesTable.update({ WorkspacesTable.id eq id }) {
            it[WorkspacesTable.ownerId] = ownerId
        }
        Unit
    }

    override suspend fun findById(id: UUID): Workspace? = dbQuery {
        WorkspacesTable.selectAll().where { WorkspacesTable.id eq id }
            .singleOrNull()?.toWorkspace()
    }

    override suspend fun findBySlug(slug: String): Workspace? = dbQuery {
        WorkspacesTable.selectAll().where { WorkspacesTable.slug eq slug }
            .singleOrNull()?.toWorkspace()
    }

    override suspend fun listAll(): List<Workspace> = dbQuery {
        WorkspacesTable.selectAll().map { it.toWorkspace() }
    }

    private fun ResultRow.toWorkspace() = Workspace(
        id = this[WorkspacesTable.id],
        name = this[WorkspacesTable.name],
        slug = this[WorkspacesTable.slug],
        ownerId = this[WorkspacesTable.ownerId],
        createdAt = this[WorkspacesTable.createdAt],
    )
}
