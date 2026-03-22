package com.appdist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val buildId: String,
    val localPath: String?,
    val state: String,   // downloading / completed / failed
    val progress: Float,
    val bytesLoaded: Long,
    val totalBytes: Long,
    val startedAt: Long
)
