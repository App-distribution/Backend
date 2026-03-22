package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object DownloadEventsTable : Table("download_events") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val buildId = uuid("build_id").references(BuildsTable.id)
    val downloadedAt = timestamp("downloaded_at")
    override val primaryKey = PrimaryKey(id)
}
