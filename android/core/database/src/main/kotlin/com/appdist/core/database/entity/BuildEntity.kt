package com.appdist.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "builds", indices = [Index(value = ["projectId"])])
data class BuildEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val environment: String,
    val changelog: String?,
    val fileSize: Long,
    val checksumSha256: String,
    val status: String,
    val isLatestInChannel: Boolean,
    val uploadDate: Long,
    val uploaderName: String,
    val cachedAt: Long
)
