package com.appdist.feature.home.data

import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.entity.BuildEntity
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.BuildResponse
import com.appdist.feature.home.domain.HomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val buildDao: BuildDao,
    private val api: ApiService
) : HomeRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun getRecentBuilds(limit: Int): Flow<List<BuildUi>> =
        buildDao.getRecentBuilds(limit)
            .map { entities -> entities.map { it.toUi() } }
            .onStart { refreshFromNetwork(limit) }

    private fun refreshFromNetwork(limit: Int) {
        scope.launch {
            try {
                val response = api.getRecentBuilds(limit)
                if (response.isSuccessful) {
                    val entities = response.body()?.map { it.toEntity() } ?: return@launch
                    buildDao.upsertBuilds(entities)
                }
            } catch (_: Exception) {
                // Network failure — serve cached data
            }
        }
    }

    private fun BuildEntity.toUi() = BuildUi(
        id = id,
        projectId = projectId,
        projectName = "",
        packageName = "",
        versionName = versionName,
        versionCode = versionCode,
        channel = channel,
        environment = environment,
        buildType = "debug",
        changelog = changelog,
        fileSize = fileSize,
        checksumSha256 = checksumSha256,
        status = status,
        isLatestInChannel = isLatestInChannel,
        uploadDate = uploadDate,
        uploaderName = uploaderName,
        minSdk = 24,
        targetSdk = 35,
        certFingerprint = null,
        abis = emptyList(),
        expiryDate = null,
        branch = null,
        commitHash = null,
        installStatus = InstallStatus.NotInstalled
    )

    private fun BuildResponse.toEntity() = BuildEntity(
        id = id,
        projectId = projectId,
        versionName = versionName,
        versionCode = versionCode,
        channel = channel,
        environment = environment,
        changelog = changelog,
        fileSize = fileSize,
        checksumSha256 = checksumSha256,
        status = status,
        isLatestInChannel = isLatestInChannel,
        uploadDate = uploadDate,
        uploaderName = uploaderName,
        cachedAt = System.currentTimeMillis()
    )
}
