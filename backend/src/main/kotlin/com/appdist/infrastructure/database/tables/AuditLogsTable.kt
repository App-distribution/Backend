package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AuditLogsTable : Table("audit_logs") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val action = varchar("action", 100)
    val resourceType = varchar("resource_type", 100)
    val resourceId = uuid("resource_id").nullable()
    val metadata = text("metadata").nullable()  // JSON
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
