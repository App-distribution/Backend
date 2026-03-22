package com.appdist.feature.builddetail.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.entity.DownloadEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_BUILD_ID = "build_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_EXPECTED_CHECKSUM = "expected_checksum"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES_LOADED = "bytes_loaded"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val buildId = inputData.getString(KEY_BUILD_ID) ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        val totalBytes = inputData.getLong(KEY_FILE_SIZE, 0L)

        val outputFile = File(applicationContext.cacheDir, "apk_downloads/$buildId.apk")
            .also { it.parentFile?.mkdirs() }

        try {
            downloadDao.upsert(DownloadEntity(buildId, null, "downloading", 0f, 0L, totalBytes, System.currentTimeMillis()))

            val request = Request.Builder().url(downloadUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.retry()
                val body = response.body ?: return@withContext Result.retry()

                outputFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                            setProgress(workDataOf(KEY_PROGRESS to progress, KEY_BYTES_LOADED to totalRead))
                            downloadDao.upsert(DownloadEntity(buildId, null, "downloading", progress, totalRead, totalBytes, System.currentTimeMillis()))
                        }
                    }
                }
            }

            downloadDao.upsert(DownloadEntity(buildId, outputFile.absolutePath, "completed", 1f, totalBytes, totalBytes, System.currentTimeMillis()))
            Result.success(workDataOf(KEY_OUTPUT_PATH to outputFile.absolutePath))
        } catch (e: Exception) {
            downloadDao.upsert(DownloadEntity(buildId, null, "failed", 0f, 0L, totalBytes, System.currentTimeMillis()))
            Result.retry()
        }
    }
}
