package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.User
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.UserRepository
import com.appdist.infrastructure.database.tables.UsersTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun findById(id: UUID): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq id }
            .singleOrNull()?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }
            .singleOrNull()?.toUser()
    }

    override suspend fun findAllByWorkspace(workspaceId: UUID): List<User> = dbQuery {
        UsersTable.selectAll().where { UsersTable.workspaceId eq workspaceId }
            .map { it.toUser() }
    }

    override suspend fun create(workspaceId: UUID, email: String, name: String, role: UserRole): User {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        dbQuery {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.workspaceId] = workspaceId
                it[UsersTable.email] = email
                it[UsersTable.name] = name
                it[UsersTable.role] = role.name
                it[UsersTable.fcmToken] = null
                it[UsersTable.createdAt] = now
            }
        }
        return User(id, workspaceId, email, name, role, null, now)
    }

    override suspend fun updateFcmToken(userId: UUID, token: String?) = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[fcmToken] = token
        }
        Unit
    }

    override suspend fun update(userId: UUID, name: String): User = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.name] = name
        }
        UsersTable.selectAll().where { UsersTable.id eq userId }.single().toUser()
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        workspaceId = this[UsersTable.workspaceId],   // UUID? — nullable column
        email = this[UsersTable.email],
        name = this[UsersTable.name],
        role = UserRole.valueOf(this[UsersTable.role]),
        fcmToken = this[UsersTable.fcmToken],
        createdAt = this[UsersTable.createdAt],
    )
}
