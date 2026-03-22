package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.Project
import com.appdist.domain.repository.ProjectRepository
import com.appdist.infrastructure.database.tables.ProjectsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ProjectRepositoryImpl : ProjectRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(workspaceId: UUID, name: String, packageName: String): Project {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        dbQuery {
            ProjectsTable.insert {
                it[ProjectsTable.id] = id
                it[ProjectsTable.workspaceId] = workspaceId
                it[ProjectsTable.name] = name
                it[ProjectsTable.packageName] = packageName
                it[ProjectsTable.iconUrl] = null
                it[ProjectsTable.createdAt] = now
            }
        }
        return Project(id, workspaceId, name, packageName, null, now)
    }

    override suspend fun findById(id: UUID): Project? = dbQuery {
        ProjectsTable.selectAll().where { ProjectsTable.id eq id }
            .singleOrNull()?.toProject()
    }

    override suspend fun listByWorkspace(workspaceId: UUID): List<Project> = dbQuery {
        ProjectsTable.selectAll().where { ProjectsTable.workspaceId eq workspaceId }
            .map { it.toProject() }
    }

    override suspend fun delete(projectId: UUID) = dbQuery {
        ProjectsTable.deleteWhere { ProjectsTable.id eq projectId }
        Unit
    }

    private fun ResultRow.toProject() = Project(
        id = this[ProjectsTable.id],
        workspaceId = this[ProjectsTable.workspaceId],
        name = this[ProjectsTable.name],
        packageName = this[ProjectsTable.packageName],
        iconUrl = this[ProjectsTable.iconUrl],
        createdAt = this[ProjectsTable.createdAt],
    )
}
