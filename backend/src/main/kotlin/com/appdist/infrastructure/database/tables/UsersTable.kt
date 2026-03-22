package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id").references(WorkspacesTable.id).nullable()
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val role = varchar("role", 50)
    val fcmToken = varchar("fcm_token", 512).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
