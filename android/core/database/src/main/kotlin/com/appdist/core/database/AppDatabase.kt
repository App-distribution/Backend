package com.appdist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity

@Database(
    entities = [BuildEntity::class, InstallHistoryEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun buildDao(): BuildDao
    abstract fun installHistoryDao(): InstallHistoryDao
    abstract fun downloadDao(): DownloadDao
}
