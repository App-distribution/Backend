package com.appdist.feature.builddetail.data

import android.os.Build
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.common.toAppError
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.BuildResponse
import com.appdist.feature.builddetail.domain.BuildDetailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildDetailRepositoryImpl @Inject constructor(
    private val api: ApiService
) : BuildDetailRepository {

    override fun getBuildDetail(buildId: String): Flow<BuildUi> = flow {
        val response = api.getBuild(buildId)
        if (response.isSuccessful) {
            val body = response.body() ?: error("Empty body for build $buildId")
            emit(body.toUi())
        } else {
            error("HTTP ${response.code()}: ${response.message()}")
        }
    }

    override suspend fun getDownloadUrl(buildId: String): Result<String> = try {
        val response = api.getDownloadUrl(buildId)
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Result.Error(AppError.Network(response.code(), "Empty body"))
            Result.Success(body.url)
        } else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(e.toAppError())
    }

    override suspend fun reportInstall(buildId: String): Result<Unit> = try {
        val request = com.appdist.core.network.dto.InstallEventRequest(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT
        )
        val response = api.reportInstall(buildId, request)
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(e.toAppError())
    }

    private fun BuildResponse.toUi() = BuildUi(
        id = id, projectId = projectId, projectName = "", packageName = "",
        versionName = versionName, versionCode = versionCode,
        channel = channel, environment = environment, buildType = buildType,
        changelog = changelog, fileSize = fileSize, checksumSha256 = checksumSha256,
        status = status, isLatestInChannel = isLatestInChannel,
        uploadDate = uploadDate, uploaderName = uploaderName,
        minSdk = minSdk, targetSdk = targetSdk, certFingerprint = certFingerprint,
        abis = abis ?: emptyList(), expiryDate = expiryDate,
        branch = branch, commitHash = commitHash,
        installStatus = InstallStatus.NotInstalled
    )
}
