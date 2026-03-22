package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ProjectsTable : Table("projects") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id").references(WorkspacesTable.id)
    val name = varchar("name", 255)
    val packageName = varchar("package_name", 255)
    val iconUrl = varchar("icon_url", 1024).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
