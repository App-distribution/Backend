package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.OtpCode
import com.appdist.domain.repository.OtpRepository
import com.appdist.infrastructure.database.tables.OtpCodesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.minutes

class OtpRepositoryImpl : OtpRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(email: String, code: String, ttlMinutes: Long): OtpCode {
        val expiresAt = Clock.System.now() + ttlMinutes.minutes
        dbQuery {
            OtpCodesTable.deleteWhere { OtpCodesTable.email eq email }
            OtpCodesTable.insert {
                it[OtpCodesTable.email] = email
                it[OtpCodesTable.code] = code
                it[OtpCodesTable.expiresAt] = expiresAt
                it[OtpCodesTable.used] = false
            }
        }
        return OtpCode(email, code, expiresAt, false)
    }

    override suspend fun findValid(email: String, code: String): OtpCode? = dbQuery {
        val now = Clock.System.now()
        OtpCodesTable.selectAll()
            .where {
                (OtpCodesTable.email eq email) and
                (OtpCodesTable.code eq code) and
                (OtpCodesTable.used eq false) and
                (OtpCodesTable.expiresAt greater now)
            }
            .singleOrNull()?.let {
                OtpCode(
                    it[OtpCodesTable.email], it[OtpCodesTable.code],
                    it[OtpCodesTable.expiresAt], it[OtpCodesTable.used]
                )
            }
    }

    override suspend fun markUsed(email: String, code: String) = dbQuery {
        OtpCodesTable.update({ (OtpCodesTable.email eq email) and (OtpCodesTable.code eq code) }) {
            it[used] = true
        }
        Unit
    }

    override suspend fun deleteExpired() = dbQuery {
        OtpCodesTable.deleteWhere { expiresAt less Clock.System.now() }
        Unit
    }
}
