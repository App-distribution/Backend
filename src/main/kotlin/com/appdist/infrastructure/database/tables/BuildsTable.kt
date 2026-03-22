package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object BuildsTable : Table("builds") {
    val id = uuid("id")
    val projectId = uuid("project_id").references(ProjectsTable.id)
    val versionName = varchar("version_name", 100)
    val versionCode = long("version_code")
    val buildNumber = varchar("build_number", 100).nullable()
    val flavor = varchar("flavor", 100).nullable()
    val buildType = varchar("build_type", 50)
    val environment = varchar("environment", 50)
    val channel = varchar("channel", 50)
    val branch = varchar("branch", 255).nullable()
    val commitHash = varchar("commit_hash", 64).nullable()
    val uploaderId = uuid("uploader_id").references(UsersTable.id)
    val uploadDate = timestamp("upload_date")
    val changelog = text("changelog").nullable()
    val fileSizeBytes = long("file_size_bytes")
    val checksumSha256 = varchar("checksum_sha256", 64).uniqueIndex()
    val minSdk = integer("min_sdk")
    val targetSdk = integer("target_sdk")
    val certFingerprint = varchar("cert_fingerprint", 64).nullable()
    val abis = text("abis")   // JSON array stored as text
    val storageKey = varchar("storage_key", 1024)
    val status = varchar("status", 50)
    val expiryDate = timestamp("expiry_date").nullable()
    val isLatestInChannel = bool("is_latest_in_channel")
    override val primaryKey = PrimaryKey(id)
}
