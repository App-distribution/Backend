package com.appdist.core.database.dao

import androidx.room.*
import com.appdist.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE state = 'downloading' OR state = 'completed'")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE buildId = :buildId")
    suspend fun getByBuildId(buildId: String): DownloadEntity?

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE buildId = :buildId")
    suspend fun delete(buildId: String)
}
