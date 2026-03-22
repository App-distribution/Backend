package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object WorkspacesTable : Table("workspaces") {
    val id = uuid("id")
    val name = varchar("name", 255)
    val slug = varchar("slug", 100).uniqueIndex()
    val ownerId = uuid("owner_id").nullable()   // set after User creation
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
