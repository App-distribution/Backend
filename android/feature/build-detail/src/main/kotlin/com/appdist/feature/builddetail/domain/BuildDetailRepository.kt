package com.appdist.feature.builddetail.domain

import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import kotlinx.coroutines.flow.Flow

interface BuildDetailRepository {
    fun getBuildDetail(buildId: String): Flow<BuildUi>
    suspend fun getDownloadUrl(buildId: String): Result<String>
    suspend fun reportInstall(buildId: String): Result<Unit>
}
