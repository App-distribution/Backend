package com.appdist.feature.browse.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.BuildResponse
import com.appdist.feature.browse.ui.builds.BuildFilters

class BuildsPagingSource(
    private val api: ApiService,
    private val projectId: String,
    private val filters: BuildFilters
) : PagingSource<Int, BuildUi>() {

    override fun getRefreshKey(state: PagingState<Int, BuildUi>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BuildUi> {
        val page = params.key ?: 0
        return try {
            val response = api.getBuilds(
                projectId = projectId,
                channel = filters.channel,
                env = filters.environment,
                search = filters.searchQuery.takeIf { it.isNotBlank() },
                page = page,
                limit = params.loadSize
            )
            if (response.isSuccessful) {
                val items = response.body()?.map { it.toUi() } ?: emptyList()
                LoadResult.Page(
                    data = items,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (items.isEmpty()) null else page + 1
                )
            } else {
                LoadResult.Error(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
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
