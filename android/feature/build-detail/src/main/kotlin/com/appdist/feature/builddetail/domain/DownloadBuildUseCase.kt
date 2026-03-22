package com.appdist.feature.builddetail.domain

import androidx.work.*
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.feature.builddetail.data.DownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float, val bytesLoaded: Long) : DownloadState
    data object Verifying : DownloadState
    data class ReadyToInstall(val filePath: String) : DownloadState
    data class Failed(val reason: AppError) : DownloadState
}

class DownloadBuildUseCase @Inject constructor(
    private val workManager: WorkManager,
    private val verifyChecksum: VerifyChecksumUseCase
) {
    operator fun invoke(build: BuildUi, downloadUrl: String): Flow<DownloadState> {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                DownloadWorker.KEY_BUILD_ID to build.id,
                DownloadWorker.KEY_DOWNLOAD_URL to downloadUrl,
                DownloadWorker.KEY_EXPECTED_CHECKSUM to build.checksumSha256,
                DownloadWorker.KEY_FILE_SIZE to build.fileSize
            ))
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .build()

        workManager.enqueueUniqueWork(
            "download_${build.id}",
            ExistingWorkPolicy.KEEP,
            request
        )

        return workManager.getWorkInfoByIdFlow(request.id).map { info ->
            when (info?.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = info.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f)
                    val bytes = info.progress.getLong(DownloadWorker.KEY_BYTES_LOADED, 0L)
                    DownloadState.Downloading(progress, bytes)
                }
                WorkInfo.State.SUCCEEDED -> {
                    val path = info.outputData.getString(DownloadWorker.KEY_OUTPUT_PATH)
                    if (path != null) {
                        val file = java.io.File(path)
                        val verified = verifyChecksum(file, build.checksumSha256)
                        if (verified.isSuccess) DownloadState.ReadyToInstall(path)
                        else DownloadState.Failed((verified as Result.Error<*>).error)
                    } else DownloadState.Failed(AppError.Unknown("No output path"))
                }
                WorkInfo.State.FAILED -> DownloadState.Failed(AppError.Unknown("Download failed"))
                else -> DownloadState.Idle
            }
        }
    }
}
