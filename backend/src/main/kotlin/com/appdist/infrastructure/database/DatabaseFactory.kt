package com.appdist.infrastructure.database

import com.appdist.config.AppConfig
import com.appdist.infrastructure.database.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: AppConfig.DatabaseConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            driverClassName = "org.postgresql.Driver"
        }
        Database.connect(HikariDataSource(hikariConfig))
        createTables()
    }

    fun initH2() {
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver"
        )
    }

    private fun createTables() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                WorkspacesTable, UsersTable, ProjectsTable, BuildsTable,
                OtpCodesTable, RefreshTokensTable, InstallEventsTable,
                DownloadEventsTable, AuditLogsTable
            )
        }
    }
}
