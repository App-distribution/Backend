package com.appdist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "install_history")
data class InstallHistoryEntity(
    @PrimaryKey val id: String,
    val buildId: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val installedAt: Long
)
