package com.appdist.core.database.dao

import androidx.room.*
import com.appdist.core.database.entity.InstallHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallHistoryDao {
    @Query("SELECT * FROM install_history ORDER BY installedAt DESC")
    fun getAll(): Flow<List<InstallHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: InstallHistoryEntity)

    @Query("SELECT * FROM install_history WHERE packageName = :packageName ORDER BY installedAt DESC LIMIT 1")
    suspend fun getLatestForPackage(packageName: String): InstallHistoryEntity?
}
