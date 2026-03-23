package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.RefreshToken
import com.appdist.domain.repository.RefreshTokenRepository
import com.appdist.infrastructure.database.tables.RefreshTokensTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.time.Duration.Companion.days

class RefreshTokenRepositoryImpl : RefreshTokenRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(userId: UUID, token: String, ttlDays: Long): RefreshToken {
        val id = UUID.randomUUID()
        val expiresAt = Clock.System.now() + ttlDays.days
        dbQuery {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.id] = id
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.token] = token
                it[RefreshTokensTable.expiresAt] = expiresAt
                it[RefreshTokensTable.revoked] = false
            }
        }
        return RefreshToken(id, userId, token, expiresAt, false)
    }

    override suspend fun findValid(token: String): RefreshToken? = dbQuery {
        val now = Clock.System.now()
        RefreshTokensTable.selectAll()
            .where {
                (RefreshTokensTable.token eq token) and
                (RefreshTokensTable.revoked eq false) and
                (RefreshTokensTable.expiresAt greater now)
            }
            .singleOrNull()?.let {
                RefreshToken(
                    it[RefreshTokensTable.id], it[RefreshTokensTable.userId],
                    it[RefreshTokensTable.token], it[RefreshTokensTable.expiresAt],
                    it[RefreshTokensTable.revoked]
                )
            }
    }

    override suspend fun revoke(token: String) = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.token eq token }) {
            it[revoked] = true
        }
        Unit
    }

    override suspend fun revokeAllForUser(userId: UUID) = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.userId eq userId }) {
            it[revoked] = true
        }
        Unit
    }
}
