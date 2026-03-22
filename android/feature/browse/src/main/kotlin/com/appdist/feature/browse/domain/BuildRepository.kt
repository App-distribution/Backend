package com.appdist.feature.browse.domain

import androidx.paging.PagingData
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.feature.browse.ui.builds.BuildFilters
import kotlinx.coroutines.flow.Flow

interface BuildRepository {
    fun getBuilds(projectId: String, filters: BuildFilters): Flow<PagingData<BuildUi>>
    suspend fun getBuild(buildId: String): Result<BuildUi>
    suspend fun getDownloadUrl(buildId: String): Result<String>
}
