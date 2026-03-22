package com.appdist.feature.settings.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncBuildsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ApiService,
    private val buildDao: BuildDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "sync_builds_periodic"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncBuildsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints(NetworkType.CONNECTED))
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val response = api.getRecentBuilds(50)
            if (response.isSuccessful) {
                val builds = response.body()?.map { dto ->
                    BuildEntity(
                        id = dto.id, projectId = dto.projectId,
                        versionName = dto.versionName, versionCode = dto.versionCode,
                        channel = dto.channel, environment = dto.environment,
                        changelog = dto.changelog, fileSize = dto.fileSize,
                        checksumSha256 = dto.checksumSha256, status = dto.status,
                        isLatestInChannel = dto.isLatestInChannel,
                        uploadDate = dto.uploadDate, uploaderName = dto.uploaderName,
                        cachedAt = System.currentTimeMillis()
                    )
                } ?: emptyList()
                buildDao.upsertBuilds(builds)
                // Stale cache cleanup: remove entries older than 7 days
                buildDao.deleteStale(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
                Result.success()
            } else {
                // 4xx: bad request, don't retry; 5xx: server error, retry
                if (response.code() in 400..499) Result.failure() else Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
