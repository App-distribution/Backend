package com.appdist.feature.upload.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_APK_PATH = "apk_path"
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_CHANNEL = "channel"
        const val KEY_CHANGELOG = "changelog"
        const val KEY_PROGRESS = "progress"
        const val KEY_BASE_URL = "base_url"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apkPath = inputData.getString(KEY_APK_PATH) ?: return@withContext Result.failure()
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: ""
        val channel = inputData.getString(KEY_CHANNEL) ?: "internal"
        val changelog = inputData.getString(KEY_CHANGELOG) ?: ""
        val baseUrl = inputData.getString(KEY_BASE_URL) ?: return@withContext Result.failure()

        val apkFile = File(apkPath)
        if (!apkFile.exists()) return@withContext Result.failure()

        try {
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
            if (projectId.isNotBlank()) multipartBuilder.addFormDataPart("project_id", projectId)
            multipartBuilder
                .addFormDataPart("channel", channel)
                .addFormDataPart("changelog", changelog)
                .addFormDataPart(
                    "apk", apkFile.name,
                    apkFile.asRequestBody("application/vnd.android.package-archive".toMediaType())
                )

            val request = Request.Builder()
                .url("${baseUrl}api/v1/builds/upload")
                .post(multipartBuilder.build())
                .build()

            setProgress(workDataOf(KEY_PROGRESS to 0.0f))
            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    setProgress(workDataOf(KEY_PROGRESS to 1.0f))
                    Result.success()
                }
                else if (resp.code in 400..499) Result.failure()
                else Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
