package com.appdist.feature.upload.domain

import androidx.work.*
import com.appdist.core.common.AppError
import com.appdist.feature.upload.data.UploadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

sealed interface UploadState {
    data class Uploading(val progress: Float) : UploadState
    data object Success : UploadState
    data class Failed(val error: AppError) : UploadState
}

class UploadBuildUseCase @Inject constructor(
    private val workManager: WorkManager,
    @Named("api_base_url") private val baseUrl: String
) {
    operator fun invoke(
        apkPath: String,
        projectId: String,
        channel: String,
        changelog: String
    ): Flow<UploadState> {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(
                UploadWorker.KEY_APK_PATH to apkPath,
                UploadWorker.KEY_PROJECT_ID to projectId,
                UploadWorker.KEY_CHANNEL to channel,
                UploadWorker.KEY_CHANGELOG to changelog,
                UploadWorker.KEY_BASE_URL to baseUrl
            ))
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .build()

        workManager.enqueueUniqueWork("upload_apk", ExistingWorkPolicy.REPLACE, request)

        return workManager.getWorkInfoByIdFlow(request.id).map { info ->
            when (info?.state) {
                WorkInfo.State.RUNNING -> UploadState.Uploading(
                    info.progress.getFloat(UploadWorker.KEY_PROGRESS, 0f)
                )
                WorkInfo.State.SUCCEEDED -> UploadState.Success
                WorkInfo.State.FAILED -> UploadState.Failed(AppError.Unknown("Upload failed"))
                else -> UploadState.Uploading(0f)
            }
        }
    }
}
