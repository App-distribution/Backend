package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ApiKeysTable : Table("api_keys") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id").references(WorkspacesTable.id)
    val createdBy = uuid("created_by").references(UsersTable.id)
    val name = varchar("name", 128)
    // Хранится только хеш: сам ключ показывается один раз при выпуске.
    val keyHash = varchar("key_hash", 64).uniqueIndex()
    val prefix = varchar("prefix", 16)
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at").nullable()
    val revoked = bool("revoked")
    override val primaryKey = PrimaryKey(id)
}
