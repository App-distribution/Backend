package com.appdist

import com.appdist.infrastructure.database.tables.OtpCodesTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/** Достаёт последний выданный код прямо из базы: по HTTP он не возвращается. */
object TestOtp {
    suspend fun lastCodeFor(email: String): String =
        newSuspendedTransaction(Dispatchers.IO) {
            OtpCodesTable.selectAll()
                .where { OtpCodesTable.email eq email }
                .orderBy(OtpCodesTable.expiresAt to SortOrder.DESC)
                .first()[OtpCodesTable.code]
        }
}
