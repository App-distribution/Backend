package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object InstallEventsTable : Table("install_events") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val buildId = uuid("build_id").references(BuildsTable.id)
    val installedAt = timestamp("installed_at")
    val deviceModel = varchar("device_model", 255).nullable()
    val androidVersion = varchar("android_version", 50).nullable()
    val manufacturer = varchar("manufacturer", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}
