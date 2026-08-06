package com.appdist

import com.appdist.infrastructure.database.DatabaseFactory
import com.appdist.infrastructure.database.tables.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object TestDatabase {
    fun init() {
        DatabaseFactory.initH2()
        transaction {
            SchemaUtils.create(
                WorkspacesTable, UsersTable, ProjectsTable, BuildsTable,
                OtpCodesTable, RefreshTokensTable, InstallEventsTable,
                DownloadEventsTable, AuditLogsTable, ApiKeysTable
            )
        }
    }

    fun reset() {
        transaction {
            SchemaUtils.drop(
                InstallEventsTable, DownloadEventsTable, AuditLogsTable,
                RefreshTokensTable, OtpCodesTable, BuildsTable,
                ProjectsTable, UsersTable, WorkspacesTable, ApiKeysTable,
            )
            SchemaUtils.create(
                WorkspacesTable, UsersTable, ProjectsTable, BuildsTable,
                OtpCodesTable, RefreshTokensTable, InstallEventsTable,
                DownloadEventsTable, AuditLogsTable, ApiKeysTable,
            )
        }
    }
}
