package com.appdist.feature.browse.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.network.ApiService
import com.appdist.feature.browse.domain.BuildRepository
import com.appdist.feature.browse.ui.builds.BuildFilters
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildRepositoryImpl @Inject constructor(
    private val api: ApiService
) : BuildRepository {

    override fun getBuilds(projectId: String, filters: BuildFilters): Flow<PagingData<BuildUi>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { BuildsPagingSource(api, projectId, filters) }
        ).flow

    override suspend fun getBuild(buildId: String): Result<BuildUi> = try {
        val response = api.getBuild(buildId)
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Result.Error(AppError.Network(response.code(), "Empty response body"))
            Result.Success(body.toUi())
        } else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e.message ?: "Unknown error"))
    }

    override suspend fun getDownloadUrl(buildId: String): Result<String> = try {
        val response = api.getDownloadUrl(buildId)
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Result.Error(AppError.Network(response.code(), "Empty response body"))
            Result.Success(body.url)
        } else Result.Error(AppError.Network(response.code(), response.message()))
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e.message ?: "Unknown error"))
    }

}
